(ns kavel.core
  "Generate and edit AI images from Clojure with no API key and no account.

    (require '[kavel.core :as kavel])
    (kavel/generate \"a paper boat at sunrise, soft fog, 35mm film\")
    ;; => {:url \"https://cdn.kavel.ai/uploads/kie/image/....webp\" :watermarked? true}

  Without a key it calls the anonymous tier of https://www.kavel.ai, so the
  first call works on a machine with nothing configured. With :api-key (or the
  KAVEL_API_KEY environment variable) every call runs on your account.

  Failures throw ex-info whose data carries :reason, one of :quota, :rejected,
  :sign-in, :auth, :timeout or :other, so a caller can tell waiting from
  rewording from signing in."
  (:import (io.github.hanshs474.kavel Kavel Kavel$Builder Kavel$Image KavelException)
           (java.net.http HttpClient)
           (java.time Duration)))

(defn- client
  ^Kavel [{:keys [api-key model timeout-ms poll-ms http-client]}]
  (let [^Kavel$Builder b (Kavel/builder)]
    (when api-key (.apiKey b api-key))
    (when model (.model b model))
    (when timeout-ms (.timeout b (Duration/ofMillis timeout-ms)))
    (when poll-ms (.pollEvery b (Duration/ofMillis poll-ms)))
    (when http-client (.httpClient b ^HttpClient http-client))
    (.build b)))

(defn- image->map [^Kavel$Image img]
  {:url (.url img) :watermarked? (.watermarked img)})

(defn- rethrow [^KavelException e]
  (throw (ex-info (.getMessage e)
                  {:reason (keyword (.replace (.toLowerCase (str (.reason e))) "_" "-"))}
                  e)))

(defn generate
  "Turns prompt into a new image and returns {:url :watermarked?}.

  opts: :aspect-ratio (\"1:1\" \"16:9\" \"9:16\" \"4:3\" \"3:4\", default \"1:1\"),
  :api-key, :model (honoured only with a key), :timeout-ms, :poll-ms,
  :http-client (a java.net.http.HttpClient, for a proxy).

  Name the light, the material and the composition. \"matte black ceramic mug
  on pale oak, soft window light from the left\" gives the model a picture,
  \"a product photo of a mug\" does not."
  ([prompt] (generate prompt {}))
  ([prompt opts]
   (try
     (image->map (.generate (client opts) prompt (:aspect-ratio opts "1:1")))
     (catch KavelException e (rethrow e)))))

(defn edit
  "Rewrites the image at source-url following instruction and returns
  {:url :watermarked?}. Say what must stay as well as what changes.

  An edit costs more than the free allowance, so without an :api-key this
  throws with :reason :quota before anything is charged."
  ([source-url instruction] (edit source-url instruction {}))
  ([source-url instruction opts]
   (try
     (image->map (.edit (client opts) source-url instruction))
     (catch KavelException e (rethrow e)))))

(defn credits
  "What a fresh anonymous client id is granted, as {:remaining :grant}. Free to call."
  []
  (try
    (let [c (.credits (Kavel/create))]
      {:remaining (.remaining c) :grant (.grant c)})
    (catch KavelException e (rethrow e))))
