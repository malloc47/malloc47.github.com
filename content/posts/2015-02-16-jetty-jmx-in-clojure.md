{:layout :post
 :title "Jetty JMX in Clojure"
 :date "2015-02-16"}

Embedded Jetty is one of the more popular servers for ring
applications.  [JMX][1] can be useful for poking around the guts of
Jetty, as well as making runtime config changes. Unfortunately,
enabling JMX for an embedded Jetty isn't a straightforward config
change, and the process for doing so in Clojure is largely
undocumented. So this is the guide that I wish existed when I found
the need to profile Jetty. If you'd rather skip the commentary, I've
put up a [minimal clojure jmx-enabled server][2] for perusal.

Most essentially, the version of Jetty that comes bundled in
`ring-jetty-adapter` is [too old][3] (currently 7.6.13) to expose
meaningful JMX hooks. Thankfully there's a [modern ring adapter][4]
that you can add to your dependency list:

```clojure
[info.sunng/ring-jetty9-adapter "0.8.1"]
```

which serves as a drop-in replacement for the official
`ring-jetty-adapter`. Another relevant dependency is Jetty's JMX
artifact:

```clojure
[org.eclipse.jetty/jetty-jmx "9.2.7.v20150116"]
```

The `jetty-jmx` version should match with the version of
`jetty-server` provided by `ring-jetty9-adapter`.

While editing `project.clj`, it's important enable JMX on the JVM
level, and select a port:

```clojure
:jvm-opts ["-Dcom.sun.management.jmxremote"
           "-Dcom.sun.management.jmxremote.ssl=false"
           "-Dcom.sun.management.jmxremote.authenticate=false"
           "-Dcom.sun.management.jmxremote.port=8001"]
```

Finally, the running Jetty server must opt-in to JMX by pointing to
the appropriate "MBean," which can be imported with:

```clojure
(ns jetty-jmx.core
  (:require [ring.adapter.jetty9 :refer [run-jetty]])
  (:import (java.lang.management ManagementFactory)
           (org.eclipse.jetty.jmx MBeanContainer)))
```

The server can then be started with:

```clojure
(let [mb-container (MBeanContainer. (ManagementFactory/getPlatformMBeanServer))]
    (doto (run-jetty app {:port 8000
                          :join? false})
      (.addEventListener mb-container)
      (.addBean mb-container)))
```

which attaches the MBean to the running Jetty server. Since the
`run-server` command calls `.start` on the `Server` object before
returning it, it's important to configure `:join?  false` to allow
thread execution to continue, preventing the following
`.addEventListener` and `.addBean` from being blocked.

With all of this, it should now be possible to start the server and
connect to the JMX port using `jconsole`:

    jconsole localhost:8001

Relevant info will be under the `MBeans` tab. Useful fields include

    org.eclipse.jetty.util.thread.queuedthreadpool.threads

for how many threads are allocated, and

    org.eclipse.jetty.util.thread.queuedthreadpool.queueSize

to find out how many requests are waiting on threads.

[1]: https://en.wikipedia.org/wiki/Java_Management_Extensions
[2]: https://github.com/malloc47/jetty-jmx
[3]: https://github.com/ring-clojure/ring/blob/master/ring-jetty-adapter/project.clj#L9
[4]: https://github.com/sunng87/ring-jetty9-adapter
