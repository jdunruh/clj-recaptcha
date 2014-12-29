(ns clj-recaptcha.client
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clj-http.conn-mgr :as manager]))

(def https-api "https://www.google.com/recaptcha/api")

(defn- parse-response
  [^String response]
  (if-not (nil? response)
    (let [return-value (clojure.data.json/read-str response :key-fn keyword)
          valid (:success return-value)
          error (:error-codes return-value)]
      {:valid? valid :error error})))


(defn verify
  "Verifies a user's answer for a reCAPTCHA challenge.

   private-key - your private key
   response    - the value of recaptcha_response_field sent via the form

   Optional parameters:
      :proxy-host         - a proxy host
      :proxy-port         - a proxy port
      :connection-manager - a connection manager to be used to speed up requests
      :remote-ip          - the IP address of the user who solved the CAPTCHA"

  [private-key response & {:keys [proxy-host proxy-port connection-manager remote-ip]}]
  (if-not (empty? response)
    (try
      (let [endpoint https-api
            endpoint (str endpoint "/verify")
            resp (client/get endpoint {:query-params       {:secret   private-key
                                                            :remoteip remote-ip
                                                            :response response}
                                       :proxy-host         proxy-host
                                       :proxy-port         proxy-port
                                       :connection-manager connection-manager})]
        (parse-response (:body resp)))
      (catch Exception ex
        {:valid? false :error "recaptcha-not-reachable"}))
    {:valid? false :error "incorrect-captcha-sol"}))

  (defn create-conn-manager
    "Creates a reusable connection manager.

     It's just a shortcut for clj-http.conn-mgr/make-reusable-conn-manager.

     Example:
         (create-conn-manager {:timeout 5 :threads 4})"
    [opts]
    (manager/make-reusable-conn-manager opts))

(defn render
    "Renders the HTML snippet to prompt reCAPTCHA.
    public-key - your public key (data-sitekey)

    Optional parameters:
        display       - a map of :symbol string attributes for reCAPTCHA custom theming (default nil)
          :data-theme dark | light - default light
          :data-type audio | imaage - default image
          :data-callback javascript callback called when successful solution"
    [public-key & {:keys [display]}]
    (str "<script src=\"https://www.google.com/recaptcha/api.js\" async defer> </script> "
    "<div class=\"g-recaptcha\" data-sitekey=\"" public-key "\" "
      (clojure.string/join " " (map #(str (name (first %)) "=\"" (second %) "\"")
                                    display)) " ></div>"))