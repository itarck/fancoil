# fancoil

> What's the frontend of duct?
>
> You say air conditioning？ Oh! Fan Coil Unit.


A clojurescript framework, which uses [multi-methods] to define and implement system unit, uses [integrant] to inject configuration and stateful dependencies to unit at system startup.

It is highly inspired by the structure of [re-frame] and [duct]. 

[integrant]:https://github.com/weavejester/integrant
[multi-methods]:https://clojure.org/about/runtime_polymorphism
[duct]:https://github.com/duct-framework/duct

## Installation

    [com.github.itarck/fancoil "0.0.7-SNAPSHOT"]    ;  Leiningen/Boot
    com.github.itarck/fancoil {:mvn/version "0.0.7-SNAPSHOT"}    ; Clojure CLI/deps.edn

## How to use

- Read the source code: not much, ~100 loc
- See [fancoil.module], if you need some module to extend
  - fancoil.module.datascript：[datascript],  an alternative to ratom
  - fancoil.module.posh：poshed datascript，you can use [posh] in subscribe
  - fancoil.module.cljs-ajax: a wrap for [cljs-ajax], as plugin of fancoil unit do!
  - fancoil.module.reitit.html-router: a [reitit] frontend router, already integranted with [accountant] and [clerk], originally from [reagent-template]
- Try some examples in [fancoil-example] repo: includes simple clock, todomvc-ratom, todomvc-datascript, cat chat (with backend via http and ws) 
  - [simple]: basic example
  - [simple-html-router]: use fancoil.module.reitit/html-router
  - [todomvc-ratom]: basic todomvc example, use ratom
  - [todomvc-posh]: use fancoil.module.posh
  - [catchat-full]: use fancoil.module.datascript, fancoil.module.cljs-ajax
  - [realworld-demo]: use fancoil.module.cljs-ajax, fancoil.module.reitit/html-router



[fancoil-example]:https://github.com/itarck/fancoil-example
[simple-html-router]:https://github.com/itarck/fancoil-example/tree/main/simple_html_router
[todomvc-posh]:https://github.com/itarck/fancoil-example/tree/main/todomvc-posh
[catchat-full]:https://github.com/itarck/fancoil-example/tree/main/catchat-full
[realworld-demo]:https://github.com/itarck/fancoil-example/tree/main/realworld
[simple]:https://github.com/itarck/fancoil-example/tree/main/simple
[todomvc-ratom]:https://github.com/itarck/fancoil-example/tree/main/todomvc-ratom
[posh]:https://github.com/denistakeda/posh
[datascript]:https://github.com/tonsky/datascript
[cljs-ajax]:https://github.com/JulianBirch/cljs-ajax
[reitit]:https://github.com/metosin/reitit
[clerk]:https://github.com/PEZ/clerk
[accountant]:https://github.com/venantius/accountant
[reagent-template]:https://github.com/reagent-project/reagent-template
[fancoil-example]:https://github.com/itarck/fancoil-example
[fancoil.module]:https://github.com/itarck/fancoil.module

## System structure

![System structure](https://github.com/itarck/fancoil/blob/main/system-structure3.jpg)


## Concept

- System
    - The system has several machines working together, and it is stateful.
    - The system needs to follow a certain order when starting the machines.
- Machine (unit)
    - Machines have three period: definition, implementation and runtime.
    - When a machine is running, it depends on other machines, and it is stateful.
    - If a machine is not running, it has no state. It is formal function that implements runtime functionality.
- Plugin
	- Plugin can extend functionality of a machine
- Module
	- Module can extend the system. Module is a package of a new machine and plugins for its related machines

## Types of machine

| Name | Homies? | Desc | Spec | Detail |
|---|---|---|---| --- |
| db | | stored state | ref | ratom，datascript |
| chan || flow state | channel | core.async.chan |
| subscribe |✅| subscribe reaction | ref -> reaction | tree of reactions |
| view |✅| view model | model -> reactions -> react component | reagent, rum |
| dispatch || dispatch event | event -> request | |
| tap |✅| tap model | value->value | user-defined, for handle, pure function |
| process || process request | request -> effect | default to db-handler |
| - inject |✅| inject co-effect | request -> request | support for multiple co-fx |
| - handle |✅| handle request | request -> response | db-handler, pure function |
| - do！ |✅| do! effect | response -> effect | support for multiple fx |
| service || long-run for request | go-loop | support for sync and async |
| schedule || once/periodic | | e.g. init process | |

## Design Pattern of "Homies"

Multimethod + integrant seems to be a kind of design pattern, I call it "Homies".  Most machines are generated in the this way. The core of a homies is a hashmap which can contain config data, internal atom, external dependencies etc.

## Life cycle of "Homies"

* Definition period: in fancoil.base, the abstraction of a machine is defined by defmulti.
    ``` clojure
    (defmulti process
        "stateful function
        request in -> effects out
        core: inject, handle, do!"
        (fn [core method & args] method))
    ```
* Implementation period: in fancoil.plugin, some methods of base are implemented, you can include them. Or you can use defmethod to implement them in your project. Method may call other methods of same multi-fn, as is common in handle and subscribe.
  ``` clojure
  (defmethod base/process :default
    [{:keys [do! handle inject] :as core} method req]
    (let [req (inject :ratom/db req)
          resp (handle method req)]
      (do! :do/effect resp)))
  ```
* Runtime period: in fancoil.unit, some integrant init-key method is implemented, and integrant will inject the configuration into the machine when it initializes the system

  ``` clojure
  (defmethod ig/init-key ::process
     [_ config]
     (partial base/process config))        
  
  (def config 
    {::process {:handle (ig/ref ::handle)
                :inject (ig/ref ::inject)
                :do! (ig/ref ::do!)}
   ;; other config })
  ```

## Features
- Separation of state: stateful dependencies are injected until last minute
- More functions: more pure or formal functions
- Highly configurable: flexibility to change the system structure via integrant config
- Highly extensible: extend for existing machines via plugins. Or write new integrant unit.
- Easy to test: use integrant to init parts of the system to do unit tests on the machine


## Credits and Thanks
- [@richhickey]:  multi-methods, a powerful runtime polymorphism
- [@weavejester]: [integrant], an elegant approach to system integration
- [@day8]: [re-frame], a clear frontend application architecture

[@richhickey]:https://github.com/richhickey
[@weavejester]:https://github.com/weavejester
[@day8]:https://github.com/day8
[re-frame]:https://github.com/day8/re-frame

## Other notes
- Still in PRE-ALPHA, some API may change.
- Request is hash-map, open. when injecting cofx, it will add namespaced key of the injector.
- Response is hash-map, open. (do! :do/effect response) can execute effect in response, but no guarantee of order. If you need to guarantee the order, use a vector of key-value pairs, just like vector form of a hash-map.
