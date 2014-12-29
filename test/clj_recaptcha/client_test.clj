(ns clj-recaptcha.client-test
  (:require [clojure.test :refer :all]
            [clj-recaptcha.client :as c]))

(deftest verify
  (testing "No response is given"
    (let [res (c/verify "KEY" nil)]
      (is (= {:valid? false :error "incorrect-captcha-sol"} res)))))

  (testing "Handles exceptions"
    (with-redefs {#'clj-http.client/get (fn [url query] (throw (Exception. "Troubles!")))}
      #(let [res (c/verify "KEY" "678")]
         (is (= {:valid? false :error "recaptcha-not-reachable"} res)))))
  
  (testing "Successful case"
    (with-redefs {#'clj-http.client/get (fn [url query]
                                              (when (and (= url "https://www.google.com/recaptcha/api/verify")
                                                         (= query {:query-params        {:secret "KEY"
                                                                                        :response   "678"}
                                                                   :proxy-host         nil
                                                                   :proxy-port         nil
                                                                   :connection-manager nil}))
                                                {:status 200
                                                 :body   "{\n\"success\": true,\n\"error-codes\": []\n}"}))}
      #(let [res (c/verify "KEY" "678")]
         (is (= {:valid? true :error nil} res)))))
  
  (testing "Incorrect user's input"
    (with-redefs-fn {#'clj-http.client/get (fn [url query]
                                              (when (and (= url "https://www.google.com/recaptcha/api/verify")
                                                         (= query {:query-params        {:secret "KEY"
                                                                                         :remoteip nil
                                                                                        :response   "678"}
                                                                   :proxy-host         "localhost"
                                                                   :proxy-port         456
                                                                   :connection-manager :CM}))
                                                {:status 200
                                                 :body   "{\n\"success\": false,\n\"error-codes\": [\"abc\" \"def\"]\n}"}))}
      #(let [res (c/verify "KEY" "678"  :proxy-host "localhost" :proxy-port 456 :connection-manager :CM)]
         (is (= {:valid? false :error ["abc" "def"]} res)))))

(deftest create-conn-manager-test
  (with-redefs-fn {#'clj-http.conn-mgr/make-reusable-conn-manager (fn [opts]
                                                                    (when (= opts {:threads 5})
                                                                      :ok))}
    #(is (= :ok (c/create-conn-manager {:threads 5})))))

(deftest render-test
  (testing "Render with default parameters"
    (let [res (c/render "234KEY")]
      (is (true? (.contains res "<div class=\"g-recaptcha\" data-sitekey=\"234KEY\""))))))


  (testing "Render with theming"
    (let [res (c/render "234KEY" :display {:theme "dark" :data-type "audio" :data-callback "This is my callback"})]
      (is (true? (.contains res "theme=\"dark\"")))
      (is (true? (.contains res "data-type=\"audio\"")))
      (is (true? (.contains res "data-callback=\"This is my callback\"")))))
