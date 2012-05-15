---
layout: post
title: Beating Interview Problems to Death with Parallel Haskell
date: 2012-05-10 00:00:00
published: false
---

Like anyone for whom graduation is becoming more immanent, I've been
taking a look at the latest trends in the typical technology interview
process.  While many of the [Fizz Buzz][1]es being thrown around
aren't exactly exciting highlights of problem solving... well, you can
always just beat them to death.

The [Run Length Encoding][2] algorithm is a nice, compact, and fairly
real-world interview problem that has been making the rounds for [years][3] 
now.  The basic idea being that "runs" of data,
e.g. `aaaabbbbbbb`, are compressed into tuples, e.g. `4a7b`, which may
be a smaller representation if there is a large amount of adjacent
repeated information.  While the real-world use cases for such a naïve
compression scheme aren't abundant, the algorithm is straightforward
and can be implemented in a dozen lines or so in most [languages][4].
If you've got regexes or library functions at your disposal, you can
manage even fewer lines.  In `Haskell`'s case, one:

{% highlight haskell %}
rleMap l = map (\e -> (head e, length e)) (group l)
{% endhighlight %}

which converts a string (or any arbitrary list of items) into a list
of tuples, each of which has the character and its count.  The
function has type

{% highlight haskell %}
rleMap :: (Eq a) => [a] -> [(a, Int)]
{% endhighlight %}

Simple and easy.  But where's the fun in calling it quits now?  
Let's [MapReduce][5] our `RLE` algorithm to make it easier to parallelize
and, potentially [Hadoop][6]-friendly.  We've already got our `map`
function, so lets create a `reduce`:

{% highlight haskell %}
rleReduce :: [(Char,Int)] -> [(Char,Int)] -> [(Char,Int)]
rleReduce [] [] = []
rleReduce a  [] = a
rleReduce [] b  = b
rleReduce a b
          | (fst $ last a ) == (fst $ head b) = 
                 init a ++  [(fst(last(a)),snd(last(a)) + snd(head(b)))] ++ tail b
          | otherwise = a ++ b
{% endhighlight %}

This is a less standard compnent of RLE implementations (I haven't
spotted this particular bit of code anywhere else), but no less
straightforward: simply join two `RLE`'d lists together if their tail
and head are not the same character; if they are, merge the head and
tail tuple (updating the count) and combine the rest of the list as
normal.

Now, it's just a matter of splitting the RLE target into pieces,
`map`ing over pieces, and `reducing` them back into a cohesive
`RLE`-encoded document:

{% highlight haskell %}
parallelRLE n s = foldl rleReduce [] $ map rleMap $ chunkn n s
{% endhighlight %}

(`chunkn` is a simple hand-rolled routine that splits a string into
`n` even-sized pices--see gist below) As expected, splitting the list
apart and recombining is needless overhead without parallelization:

    [show runtimes here]

If we parallelize it using a simple `parMap`, we might expect some
improvement:

{% highlight haskell %}
parallelRLE n s = foldl rleReduce [] $ (parMap rdeepseq) rleMap $ chunkn n s
{% endhighlight %}

But, unfortunately, the bookkeeping and garbage collection overwhelm
the problem very quickly.  

    [show lack of improvement]

I'm running the above on a few multi-megabyte text files (some with
more redundancy than others) to try it out, and no amount of coaxing
could make the parallelized version do any better.  While we could
have written our `RLE` algorithm in plain `C` without much more
trouble and not have encountered such performance obstacles, one does
not [simplly parallelize C][8] by swapping in a `parMap` either (see
[this][7]).  So, we have to deep-dive into some `Haskell` optimization
to get a performant version.

There is one painful bottleneck: `Haskell` list monads are not ideal
for handling bulk data of the sort we need, especially since
`Haskell`'s `String` type is really just a `[Char]`.  Since there's no
reason to use a boxed linked list just to scan over characters, we
instead turn to `Data.ByteString` for reading the input, and to
`Data.Sequence` to handle the RLE-encoded tuples.  `Data.Sequence`
specifically removes the large penalty when concatenating the lists
together in the `reduce` step, as adding to either end of a `Seq` is a
constant time operation, unlike `[]`, where only adding an element to
a list head is constant time.  Importing these

{% highlight haskell %}
import Data.ByteString.Lazy.Char8 as BL 
       (ByteString
       ,length
       , take
       , null
       , splitAt
       , group
       , head
       , pack
       , readFile)
import Data.Sequence as S
{% endhighlight %}

lets us rewrite our `map`

{% highlight haskell %}
rleMap :: BL.ByteString -> Seq (Char,Int)
rleMap l = fromList $ P.zip (map BL.head c) (map (fromIntegral . BL.length) c)
       where
        c = BL.group $ l
{% endhighlight %}

and `reduce`

{% highlight haskell %}
rleReduce :: Seq (Char,Int) -> Seq (Char,Int) -> Seq (Char,Int)
rleReduce a b = rleReduce' (viewr a) (viewl b)
             where
              rleReduce' EmptyR EmptyL = S.empty
              rleReduce' EmptyR _ = b
              rleReduce' _ EmptyL = a
              rleReduce' (rs :> r) (l :< ls)
                         | (fst r) == (fst l) = 
                           (rs |> (fst(r),snd(r) + snd(l))) >< ls
                         | otherwise = a >< b
{% endhighlight %}

Optionally, `Data.Sequence` has views, which is what `ViewPatterns`
was made for.  Rewriting with these in mind makes the new `reduce`
resemble the old one fairly closely:

{% highlight haskell %}
{-# LANGUAGE ViewPatterns #-}
rleReduce (viewr -> EmptyR) (viewl -> EmptyL) = S.empty
rleReduce (viewr -> EmptyR) b = b
rleReduce a (viewl -> EmptyL) = a
rleReduce a@(viewr -> (rs :> r)) b@(viewl -> (l :< ls))
           | (fst r) == (fst l) = 
             (rs |> (fst(r),snd(r) + snd(l))) >< ls
           | otherwise = a >< b
{% endhighlight %}

Now we just define a new `parallelRLE`

{% highlight haskell %}
parallelRLE :: Int -> BL.ByteString -> Seq (Char, Int)
parallelRLE n s = foldl rleReduce empty $ (parMap rseq) rleMap $ chunkn n s
{% endhighlight %}

and wrap it all up in a `IO` monad

{% highlight haskell %}
main :: IO()
main = do
     [fileName] <- getArgs
     s <- (BL.readFile fileName)
     print (parallelRLE (numCapabilities) s)
{% endhighlight %}

With an improved algorithm and `IO` wrapper, it's time for a more
complete benchmark:

    [img here]

Between 2 and 3 processors, we get a nicely ramped speedup.  After 7
processors, the bookkeeping overhead rears its ugly head again
reversing the trend, and after about 41 processors, the
parallelization ends up with a worse running time than the
nonparallelized version, with a few other spikes before this.

While I'm no `Haskell` expert, parallelization which costs no more
than swapping in a `parMap` and paying homage to the Big O gods is a
very compelling reason to hammer out any other toy interview questions
with `Haskell` in the future.

    [gist here]

[1]: http://imranontech.com/2007/01/24/using-fizzbuzz-to-find-developers-who-grok-coding/
[2]: http://en.wikipedia.org/wiki/Run-length_encoding
[3]: http://stackoverflow.com/questions/2048854/c-interview-question-run-length-coding-of-strings
[4]: http://rosettacode.org/wiki/Run-length_encoding
[5]: http://en.wikipedia.org/wiki/MapReduce
[6]: http://hadoop.apache.org/mapreduce/
[7]: http://en.wikipedia.org/wiki/There_ain%27t_no_such_thing_as_a_free_lunch
[8]: http://memegenerator.net/instance/20426610
