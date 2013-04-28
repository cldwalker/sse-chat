(ns sse-chat.render
  (:require [clojure.java.io :as io]
            [comb.template :as comb]
            [ring.util.response :as ring-resp]))

(defn- render-erb
  "Renders an erb file with optional bindings."
  ([template] (render-erb template {}))
  ([template template-bindings]
     (comb/eval (slurp (io/resource template)) template-bindings)))

(defn erb
  "Given an erb file's basename, renders it wrapped in a default layout"
  [& args]
  (let [basename (format "public/%s.erb" (name (first args)))]
    (ring-resp/response (render-erb "public/layout.erb"
                                    {:yield (apply render-erb (cons basename (rest args)))}))))
