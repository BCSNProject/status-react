(ns status-im.ui.screens.profile.seed.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [status-im.ui.components.react :as react]
            [status-im.ui.components.toolbar.view :as not.toolbar]
            [status-im.ui.components.toolbar.actions :as actions]
            [status-im.react-native.resources :as resources]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [status-im.ui.components.icons.vector-icons :as icons]
            [quo.core :as quo]
            [status-im.ui.components.common.styles :as components.common.styles]
            [status-im.ui.components.toolbar :as toolbar]
            [clojure.string :as string]
            [status-im.utils.utils :as utils]
            [status-im.ui.screens.profile.seed.styles :as styles]
            [status-im.i18n :as i18n]
            [quo.core :as quo]
            [status-im.utils.platform :as platform]))

(def steps-numbers
  {:intro       1
   :12-words    1
   :first-word  2
   :second-word 3
   :finish      3})

(defn step-back [step]
  (case step
    (:intro :12-words) (re-frame/dispatch [:navigate-back])
    :first-word (re-frame/dispatch [:my-profile/set-step :12-words])
    :second-word (re-frame/dispatch [:my-profile/set-step :first-word])))

(defn intro []
  [react/scroll-view {:style                   {:padding-horizontal 16}
                      :content-container-style {:align-items     :center
                                                :justify-content :center}}
   (when-not platform/desktop?
     [react/image {:source (resources/get-image :lock)
                   :style  styles/intro-image}])
   [react/i18n-text {:style styles/intro-text
                     :key   :your-data-belongs-to-you}]
   [react/i18n-text {:style styles/intro-description
                     :key   :your-data-belongs-to-you-description}]
   [quo/button
    {:style    styles/intro-button
     :on-press #(re-frame/dispatch [:set-in [:my-profile/seed :step] :12-words])}
    (i18n/label :t/ok-continue)]])

(defn six-words [words]
  [react/view {:style styles/six-words-container}
   (for [[i word] words]
     ^{:key (str "word" i)}
     [react/view
      [react/view {:style styles/six-word-row}
       [react/text {:style styles/six-word-num}
        (inc i)]
       [react/text {:style styles/six-words-word}
        word]]
      (when (not= i (first (last words)))
        [react/view {:style styles/six-words-separator}])])])

(defview twelve-words [{:keys [mnemonic]}]
  (letsubs [mnemonic-vec (vec (map-indexed vector (clojure.string/split mnemonic #" ")))
            ref (reagent/atom nil)]
    [react/view {:flex 1}
     [react/view {:style styles/twelve-words-container}
      [react/text {:style styles/twelve-words-label}
       (i18n/label :t/your-recovery-phrase)]
      [react/view {:style styles/twelve-words-columns
                   :ref   (partial reset! ref)}
       [six-words (subvec mnemonic-vec 0 6)]
       [react/view {:style styles/twelve-words-columns-separator}]
       [six-words (subvec mnemonic-vec 6 12)]]
      [react/text {:style styles/twelve-words-description}
       (i18n/label :t/your-recovery-phrase-description)]
      [react/view styles/twelve-words-spacer]]
     [toolbar/toolbar
      {:right
       [quo/button {:type     :secondary
                    :after    :main-icon/next
                    :on-press #(re-frame/dispatch [:my-profile/enter-two-random-words])}
        (i18n/label :t/next)]}]]))

(defn next-handler [word entered-word step]
  (fn [_]
    (cond (not= word entered-word)
          (re-frame/dispatch [:set-in [:my-profile/seed :error] (i18n/label :t/wrong-word)])

          (= :first-word step)
          (re-frame/dispatch [:my-profile/set-step :second-word])

          :else
          (utils/show-question
           (i18n/label :t/are-you-sure?)
           (i18n/label :t/are-you-sure-description)
           #(re-frame/dispatch [:my-profile/finish])))))

(defn enter-word [step [idx word] error entered-word]
  ^{:key word}
  [react/view {:flex 1}
   [react/view {:style styles/enter-word-container}
    [react/view {:style styles/enter-word-row}
     [react/text {:style styles/enter-word-label}
      (i18n/label :t/check-your-recovery-phrase)]
     [react/text {:style styles/enter-word-n}
      (i18n/label :t/word-n {:number (inc idx)})]]
    [quo/text-input
     {:placeholder       (i18n/label :t/enter-word)
      :label             [:<>
                          (i18n/label :t/check-your-recovery-phrase)
                          " "
                          [quo/text {:color :secondary}
                           (i18n/label :t/word-n {:number (inc idx)})]]
      :auto-focus        true
      :auto-correct      false
      :keyboard-type     "visible-password"
      :on-change-text    #(re-frame/dispatch [:set-in [:my-profile/seed :word] %])
      :on-submit-editing next-handler
      :error             error}]
    [react/text {:style styles/enter-word-n-description}
     (i18n/label :t/word-n-description {:number (inc idx)})]
    [react/view styles/twelve-words-spacer]]
   [toolbar/toolbar
    {:right
     [quo/button (merge {:type      :secondary
                         :disabled  (string/blank? entered-word)
                         :on-press  (next-handler word entered-word step)}
                        (when-not (= :second-word step)
                          {:after :main-icon/next}))
      (if (= :second-word step)
        (i18n/label :t/done)
        (i18n/label :t/next))]}]])

(defn finish []
  [react/view {:style styles/finish-container}
   [react/view {:style styles/finish-logo-container}
    [react/view {:style (components.common.styles/logo-container 80)}
     [icons/icon :main-icons/check styles/ok-icon]]]
   [react/text {:style styles/finish-label}
    (i18n/label :t/you-are-all-set)]
   [react/text {:style styles/finish-description}
    (i18n/label :t/you-are-all-set-description)]
   [quo/button {:style    styles/finish-button
                :on-press #(re-frame/dispatch [:navigate-back])}
    (i18n/label :t/ok-got-it)]])

(defview backup-seed []
  (letsubs [current-multiaccount [:multiaccount]
            {:keys [step first-word second-word error word]} [:my-profile/recovery]]
    [react/keyboard-avoiding-view {:style {:flex 1}}
     [not.toolbar/toolbar
      nil
      (when-not (= :finish step)
        (not.toolbar/nav-button (actions/back #(step-back step))))
      [react/view
       [react/text {:style styles/backup-seed}
        (i18n/label :t/backup-recovery-phrase)]
       [react/text {:style styles/step-n}
        (i18n/label :t/step-i-of-n {:step (steps-numbers step) :number 3})]]]
     (case step
       :intro [intro]
       :12-words [twelve-words current-multiaccount]
       :first-word [enter-word step first-word error word]
       :second-word [enter-word step second-word error word]
       :finish [finish])]))
