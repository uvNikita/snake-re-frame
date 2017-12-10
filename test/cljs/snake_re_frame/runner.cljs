(ns snake-re-frame.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [snake-re-frame.core-test]))

(doo-tests 'snake-re-frame.core-test)
