{:layout :post
 :title "Building a Personal Dashboard in ClojureScript Part 2"
 :date "2021-07-09"
 :uri "/building-a-personal-dashboard-in-clojurescript-part-2"}

Following the [previous installment] in my series on building a
dashboard in ClojureScript, I'll be diving into the weather card.

<a href="/img/posts/cockpit/weather.png">
  <img src="/img/posts/cockpit/weather.png" alt="Weather card" width="400" />
</a>

Like any re-frame application, this comes in two major pieces:
consuming from the API to update the application state, and rendering
the state on the page. Before showing how this is wired up, however,
let's first dive into the external weather API itself.

# Weather API

There are several different weather APIs with a free tier that can
handle the minimal traffic of a single dashboard. I landed on [Open
Weather Map API][], which has both a free tier and an easy-to-use [one
call endpoint][] containing all the weather granularity (current and
day/hour/minute-level) needed for a reasonable dashboard.

A sample request (with lots of fields omitted):

```bash
> curl 'http://api.openweathermap.org/data/2.5/onecall?lat=<latitude>&lon=<longitude>&units=imperial&appid=<apikey>' | jq .

{
  "current": {
    "dt": 1625517908,
    "sunrise": 1625477417,
    "sunset": 1625531408,
    "temp": 82.31,
    "feels_like": 84.31,
    "pressure": 1017,
    "humidity": 57,
    "weather": [
      {
        "id": 800,
        "main": "Clear",
        "description": "clear sky",
        "icon": "01d"
      }
    ]
    ...
  },
  "daily": [
    {
      "dt": 1625504400,
      "sunrise": 1625477417,
      "sunset": 1625531408,
      "temp": {
        "day": 83.12,
        "min": 66.2,
        "max": 83.82,
        "night": 75.74,
        "eve": 81.82,
        "morn": 67.89
      },
      "feels_like": {
        "day": 83.86,
        "night": 76.21,
        "eve": 83.5,
        "morn": 68.29
      },
      "humidity": 49,
      "weather": [
        {
          "id": 500,
          "main": "Rain",
          "description": "light rain",
          "icon": "10d"
        }
      ],
      "rain": 0.53,
      ...
    },
    ...
  ],
  "minutely": [...],
  "hourly": [...],
  "alerts": [...]
}
```

In addition, we'll want to tie the payload to a set of weather icons
supplied by the [Weather Icons][] font together using this [mapping][]
(represented below as `id->icon`).

# API Client

In re-frame parlance, we use an "effects handler" to make http calls,
which is helpfully provided by [re-frame-http-fx][]. This allows us to
define a `::fetch-weather` event analogous to the `curl` command above:

```clojure
(re-frame/reg-event-fx
 ::fetch-weather
 (fn [_ _]
   {:http-xhrio
    {:method :get
     :uri    "http://api.openweathermap.org/data/2.5/onecall"
     :params {:lat   (:lat config/home)
              :lon   (:lon config/home)
              :units "imperial"
              :appid config/open-weather-api-key}
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success      [::events/http-success [:weather]]
     :on-failure      [::events/http-fail [:weather]]}}))
```

where the success and fail events are defined as:

```clojure
(re-frame/reg-event-db
 ::http-success
 (fn [db [_ key-path result]]
   (assoc-in db key-path result)))

(re-frame/reg-event-db
 ::http-fail
 (fn [db [_ key-path]]
   (assoc-in db key-path {})))
```

We can trigger this event at regular intervals, similar to the clock
card:

```clojure
(defn init []
  ...
  (re-frame/dispatch
   [::poll/set-rules
    [{:interval                 900 ; 15 minutes
      :event                    [::weather/fetch-weather]
      :dispatch-event-on-start? true}]])
  ...)
```

The 15 minute interval is set such that the API's free tier daily
request limit is apportioned throughout the day with some headroom
remaining.

Finally, it is customary to create a "level 2" extractor subscription
to pull the payload back out of the application state even though it
is largely a trivial subscription:

```clojure
(re-frame/reg-sub
 ::weather
 (fn [db _]
   (:weather db)))
```

Getting the weather payload ensconced in `re-frame.db/app-db` with a
basic extractor is but our first step. It would be awkward for our
view to consume directly from the full API payload as it contains many
elements that would need to be filtered out or ignored; it also has
the disadvantage that re-frame would have to re-render the weather
element every time the payload is fetched even for UI elements that do
not need to change. Enter the "level 3" [materialized
view][subscriptions], which filters down the payload into meaningful
units of work. In this case, these units are:
- Sunrise and sunset time
- Current conditions
- 6 day forecast

The sunrise/sunset subscription is easy once we've defined the
`epoch->local-date` helper (that uses [cljs-time][] internally) to
parse the times into an object:

```clojure
(re-frame/reg-sub
 ::sun
 :<- [::weather]
 (fn [{{:keys [sunrise sunset]} :current} _]
   {:sunrise (-> sunrise epoch->local-date .toUsTimeString)
    :sunset  (-> sunset epoch->local-date .toUsTimeString)}))
```

The current conditions subscription is also relatively simple,
involving some light formatting (some of which could arguably be
pushed down to the view layer):

```clojure
(re-frame/reg-sub
 ::conditions
 :<- [::weather]
 (fn [{{humidity                :humidity
        feels-like              :feels_like
        current-temp            :temp
        [{:keys [description]}] :weather} :current
       [{:keys                [rain snow]
         {low :min high :max} :temp}]     :daily} _]
   {:humidity    (-> humidity (str "%"))
    :feels-like  (-> feels-like int (str "°"))
    :description (some-> description str/capitalize)
    :rain        (some-> rain mm->in (round-nonzero 2) (str "\""))
    :snow        (some-> snow mm->in (round-nonzero 2) (str "\""))
    :temp        (some-> current-temp int (str "°"))
    :low         (some-> low int (str "°"))
    :high        (some-> high int (str "°"))}))
```

This subscription plucks the current weather conditions from the
payload (using the fancy [destructuring][] that makes Clojure so
effective) and returns a new, sparser map with the values formatted
and ready to be used in a view.

The most complex subscription is the forecast, which involves
processing the `:daily` list of elements and returning a new list of
ready-to-template maps:

```clojure
(re-frame/reg-sub
 ::forecast
 :<- [::weather]
 (fn [{forecast :daily} _]
   (->> forecast
        rest                            ; skip today
        (map (fn [{date                 :dt
                   {low :min high :max} :temp
                   rain                 :rain
                   snow                 :snow
                   [{icon-id :id} & _]  :weather}]
               {:epoch   date
                :weekday (-> date
                             epoch->local-date
                             .getWeekday
                             number->weekday)
                :icon    (id->icon icon-id)
                :high    (some-> high int (str "°"))
                :low     (some-> low int (str "°"))
                :rain    (some-> rain mm->in (round-nonzero 1) (str "\""))
                :snow    (some-> snow mm->in (round-nonzero 1) (str "\""))}))
        (take 6))))
```

This is similar to the current conditions subscription above; the
major change here is that we are `map`ing over the list of forecasts
and taking only a fixed number of them.

This wraps up the event/subscription handling; with this code, we now
ingest from the API and have defined a graph of subscriptions that
whittles the payload down into filtered chunks that are ready to be
placed into our view.

# Weather Card

Creating views like the weather card is as much as art as it is an
engineering effort, and I don't expect I'd win any awards for either
aspect.

Like any normal Clojure function, breaking our view into smaller
pieces will greatly aid readability:

```clojure
(defn weather []
  [:> Card
   [:> CardContent
    [weather-description]
    [weather-conditions]
    [weather-forecast]]])
```

Like the [previous installment][], the view uses uses the
[Material-UI][] react framework (i.e., the `Card`, `CardContent`
components and many more) which comes with much saner style defaults
than any CSS I could cook up.

```clojure
(defn weather-conditions []
  [:> Grid {:container true :justify "center"}
   [:> Grid {:item true :xs 3}
    [:> Typography {:variant "h1"}
     ;; Display a large icon of current conditions
     [:i {:class (str "wi wi-"
                      @(re-frame/subscribe [::weather/icon]))}]]]
   [:> Grid {:item true :xs 5}
    [:> Typography {:align "center" :variant "h1"
                    :display "inline"}
     ;; Large view of the current temperature
     (:temp @(re-frame/subscribe [::weather/conditions]))]]
   [:> Grid {:item true :xs 2}
    (let [{:keys [low high]}
	     @(re-frame/subscribe [::weather/conditions])]
      [:> Typography {:align "right" :variant "h4"}
       high [:br] low])]])
```

With some minor extra styling, we end up with a nice, large display of
the current temperature:

<a href="/img/posts/cockpit/weather-current.png">
  <img src="/img/posts/cockpit/weather-current.png" alt="Current weather" width="400" />
</a>

Like the companion subscription, the forecast view `map`s over the
individual days in the subscribed output to produce, in this case,
`Grid` items to fill the card:

```clojure
(defn weather-forecast []
  [:> Grid {:container true}
   (map
    (fn [{:keys [epoch weekday icon high low rain snow]}]
      ^{:key epoch}
      [:> Grid {:item true :xs 2}
       [:> Typography {:key epoch
                       :variant "body1"
                       :align "center"}
        weekday]
       [:> Typography {:align "center" :variant "h5"}
        [:i {:class (str "wi wi-" icon)}]]
       [:> Typography {:align "center" :variant "subtitle2"}
        high
        (gstring/unescapeEntities "&#8194;")
        low
	    (when rain
          [:<> [:br] (list " " rain)])
        (when snow
          [:<>
            (list " " snow)])]])
    @(re-frame/subscribe [::weather/forecast]))])
```

When generating view elements dynamically, specifying the `key` is
important for re-frame (and React under-the-hood) to reliably match up
elements that must be re-rendered when the payload changes. This gives
us our 6-day forecast (which is all I could fit on the card even
though the API returns more data):

<a href="/img/posts/cockpit/weather-forecast.png">
  <img src="/img/posts/cockpit/weather-forecast.png" alt="Weather forecast" width="400" />
</a>

Last but not least, having a general text description of the weather
is handy to capture leftover details that do not appear elsewhere in
the UI:

```clojure
(defn weather-description []
  (let [{:keys [humidity feels-like description rain snow]}
        @(re-frame/subscribe [::weather/conditions])]
    (->> [{:content description :render? description}
          {:prefix "Feels like " :content feels-like :render? true}
          {:content humidity :render? true}
          {:postfix " rain" :content rain :render? rain}
          {:postfix " snow" :content snow :render? snow}]
         (map (fn [{:keys [prefix postfix content render?]}]
                (if render?
                  (->> [prefix content postfix] (remove nil?) vec)
                  [])))
         (remove empty?)
         (interpose [" | "])
         (apply concat [:> Typography {:align "center"
                                       :color "textSecondary"
                                       :variant "body1"}])
         vec)))
```

This function is more elaborate than it needs to be, but is handy for
adding new things to appear in the description--it first converts the
individual datapoints into a vector of maps that (depending on the
value of the `:render?` key) are subsequently concatenated into a
`|`-separated series of descriptions:

<a href="/img/posts/cockpit/weather-description.png">
  <img src="/img/posts/cockpit/weather-description.png" alt="Weather description" width="400" />
</a>

The full working code is available in [weather.cljs][] and
[views.cljs][] which include a few extra visual tweaks and custom
React components. Also omitted from the code in this post are a few
visual details from the screenshot above, including the "refresh"
button that triggers the `::fetch-weather` event on-demand and the
timer in the corner showing how much time has elapsed since the last
fetch--not essential features for everyday use, but valuable for
debugging.

With luck, the next post in this series will get to my favorite part
of the dashboard: the transit card.

 |---|---|---|
 | [Part 1][previous installment] | Part 2 | [Part 3][last installment] |

[previous installment]: /building-a-personal-dashboard-in-clojurescript
[last installment]: /building-a-personal-dashboard-in-clojurescript-part-3
[Open Weather Map API]: https://openweathermap.org/api
[re-frame-http-fx]: https://github.com/day8/re-frame-http-fx
[one call endpoint]: https://openweathermap.org/api/one-call-api
[Weather Icons]: https://erikflowers.github.io/weather-icons/
[mapping]: https://github.com/erikflowers/weather-icons/issues/204
[subscriptions]: https://day8.github.io/re-frame/subscriptions/
[cljs-time]: https://github.com/andrewmcveigh/cljs-time
[destructuring]: https://clojure.org/guides/destructuring
[Material-UI]: https://material-ui.com/
[weather.cljs]: https://github.com/malloc47/cockpit/blob/ac0ba2f5c7d985aceca03fdb079050d498983587/src/cljs/cockpit/weather.cljs
[views.cljs]: https://github.com/malloc47/cockpit/blob/ac0ba2f5c7d985aceca03fdb079050d498983587/src/cljs/cockpit/views.cljs
