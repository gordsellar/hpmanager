(ns hpmanager.components.shared
  "A library of helper wrappers for components"
  (:require
    [taoensso.timbre :as log]
    [re-frame.core :as rf]
    [reagent.core :as r]
    )
  )

(def button-size (atom "btn-sm"))

(defn button
  "Creates a button, re-sizes if button-size is changed"
  ([?state-map ?on-click-fn text]
   [:div.btn.btn-default
    (merge {:on-click ?on-click-fn
            :class @button-size}
           ?state-map)
    text])
  ([?on-click-fn text]
   (button nil ?on-click-fn text)))

(defn collapsable
  "Creates a collapsable panel. Each component should have a different header-text"
  [header-text & body-items]
  (let [collapsed? (rf/subscribe [::collapsed header-text])]
    (fn []
    [:div.panel.panel-default
     [:div.panel-heading
      [:div.btn.btn-default.btn-xs {:on-click #(do (log/infof "Collapsing/expanding: %s" header-text)
                                            (rf/dispatch [::toggle-maximised header-text]))}
       (if @collapsed? "+" "-")]
      \space header-text]
     (if-not @collapsed? (apply vector :div.panel-body body-items))
     ])))

(defn- list-to-map
  [l]
  (reduce merge {} (map vec (partition 2 l))))
(defn tabbed
  "Creates a tabbed window."
  [unique-id title-to-component-coll]
  (log/infof "Rendering tabbed component: %s" unique-id)
  (let [sw (rf/subscribe [::tab-switch unique-id])
        m (if (map? title-to-component-coll)
            title-to-component-coll
            (list-to-map title-to-component-coll))]
    (fn []
      [:div
       [:ul.nav.nav-tabs
        (doall (map (fn [[k _]]
                      ^{:key k}
                      [:li
                       {:class (if (= @sw k) "active" "")}
                       [:a
                        {:onClick #(rf/dispatch [::tab-switch (str unique-id) k])}
                        k]])
                    (sort-by (comp str first) m)))]
        ;; Look up the switch. If we haven't picked anything, return an empty div
        [(get m @sw :div) {:class "tab-content"}]])))

(defn debug-display
  "Displays the *entire* db map. Should be removed during debugging."
  []
  (let [debug-atom (rf/subscribe [:debug])]
    [:div.component
     [:div
     (map (fn [[k v]] ^{:key k} [collapsable k (pr-str v)]) @debug-atom)]
     [collapsable "Keys:" (interpose [:br] (keys @debug-atom))]
     [collapsable "Entire db:" (pr-str @debug-atom)]
     ]))


(rf/reg-sub
  ::collapsed
  (fn [db [_ kw]]
    (get-in db [::collapsed kw])))

(rf/reg-event-db
  ::toggle-maximised
  (fn [db [_ kw]]
    (update-in db [::collapsed kw] not)))

(rf/reg-sub
  ::tab-switch
  (fn [db [_ kw]]
    (get-in db [::tab-switch kw])))
(rf/reg-event-db
  ::tab-switch
  (fn [db [_ kw sw]]
    (log/infof "tab-switch. kw is: %s, sw is: %s" kw sw)
    (assoc-in db [::tab-switch kw] sw)))
