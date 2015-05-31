(ns pantalaimon.core
  (:require [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go go-loop chan buffer close! thread
                     alts! alts!! timeout]]))

;; this should also start the process and associate channels with it.
;; Should return a map of the channels n stuff
(defn process [name & args]
  (let [pb            (new java.lang.ProcessBuilder (concat (list name) args))
        stdout-chan   (chan)
        stderr-chan   (chan)
        stdin-chan    (chan)
        close-chans!  (fn [] (map close! [stdout-chan stderr-chan stdin-chan]))
        p             (.start pb)
        exit-future   (future
                        (.waitFor p)
                        ;(close-chans!) This isn't needed, when we close we'll get nils from the reader which will close the stream.
                        ; this way all the output goes into the buffer, even after the process has died.
                        (.exitValue p))
        stdout-stream (new java.io.BufferedReader (new java.io.InputStreamReader (.getInputStream p)))]

    (go-loop []
      (let [line (.readLine stdout-stream)]
        (>! stdout-chan line)
        (if (= line nil)
          (close-chans!)
          (recur))))

    {:process p
     :stdout stdout-chan
     :stderr stderr-chan
     :stdin stdin-chan
     :exit exit-future}
    )
  )

(defn kill [process] (.destroy (:process process)))
(defn stdout-chan [process] (:stdout process))
(defn stdin-chan [process] (:stdin process))
(defn stderr-chan [process] (:stderr process))

;; if parent dies daemon should die too
;; status
;; kill sever
;; restart?
;; log tail?
;; piping?

;(def p (process "yes"))

;(<!! (:stdout p))

