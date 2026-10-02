# kavel-clj

Generate AI images from Clojure with **no API key and no account**.

```clojure
;; deps.edn
io.github.hanshs474/kavel-clj {:mvn/version "0.1.1"}

;; Leiningen
[io.github.hanshs474/kavel-clj "0.1.1"]
```

```clojure
(require '[kavel.core :as kavel])

(kavel/generate "matte black ceramic mug on pale oak, soft window light from the left"
                {:aspect-ratio "16:9"})
;; => {:url "https://cdn.kavel.ai/uploads/kie/image/....webp" :watermarked? true}
```

Every other image client wants a key from OpenAI, fal or Replicate before it runs once. This one
calls the anonymous tier of [Kavel](https://www.kavel.ai/?utm_source=clojars&utm_medium=package),
an online AI image and video studio, so the first call works from a REPL with nothing configured.
It is a thin wrapper over the zero-dependency
[Java client](https://central.sonatype.com/artifact/io.github.hanshs474/kavel).

## Failures are data

Everything throws `ex-info` with a `:reason` you can dispatch on:

| `:reason` | What to do |
|---|---|
| `:quota` | The free allowance is spent. Wait for tomorrow or pass `:api-key` |
| `:rejected` | The prompt was refused. Reword it; retrying the same text does not help |
| `:sign-in` | The request needs an account (a model off the free shelf) |
| `:auth` | The API key is invalid |
| `:timeout` | No image before the deadline (default six minutes, because free runs queue first) |

```clojure
(try (kavel/generate "a lighthouse at dusk, oil painting")
     (catch clojure.lang.ExceptionInfo e
       (case (:reason (ex-data e))
         :quota    (println "free allowance spent for today")
         :rejected (println "reword the prompt")
         (throw e))))
```

## The free tier

`(kavel/credits)` reads what a fresh client id is granted, and the call costs nothing. On 2026-10-02 it was
5 credits, one generated image spent all five, and one IP got two images that day. Free output is
1K and watermarked.

## Editing a photo

```clojure
(kavel/edit "https://example.com/portrait.jpg"
            "shoulder-length layered haircut, keep the same face, skin and lighting"
            {:api-key (System/getenv "KAVEL_API_KEY")})
```

An edit costs more than the free grant, so it needs a key from
[kavel.ai/settings/apikeys](https://www.kavel.ai/settings/apikeys?utm_source=clojars&utm_medium=package).
Without one it throws `:quota` before anything is charged. With a key every call runs on your
account, with no watermark on a paid plan, and `:model` picks any image model on your plan.

## Options

`:aspect-ratio` (`"1:1"` `"16:9"` `"9:16"` `"4:3"` `"3:4"`) · `:api-key` · `:model` · `:timeout-ms` ·
`:poll-ms` · `:http-client` (a `java.net.http.HttpClient`, for a proxy).

## License

MIT. Generated with [Kavel AI](https://www.kavel.ai/?utm_source=clojars&utm_medium=package).
