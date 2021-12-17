# fancoil

A clojurescript framework, which uses [multi-methods] to define and implement system unit, use [integrant] to inject configuration and stateful dependencies to unit at system startup.

[integrant]:https://github.com/weavejester/integrant
[multi-methods]:https://clojure.org/about/runtime_polymorphism

## System structure

<img src="https://github.com/itarck/fancoil/blob/main/system-structure.jpg" width="400">


## Concept

- System
    - The system has several machines working together, and it is stateful.
    - The system needs to follow a certain order when starting the machines.
- Machine (system unit)
    - Machines have three period: definition, implementation and runtime.
    - When a machine is running, it depends on other machines, and it is stateful.
    - If a machine is not running, it has no state. It is formal function that implements runtime functionality.
- Plugin
	- Plugin can extend functionality of an existing machine
- Module
	- Module can extend the system by replacing existing machines or adding new machines to the system
	- Module include a new machine and plugins for existing machines

## Types of machine

| Name | Desc | Spec | Detail |
|---|---|---|---|
| db | stored state | ref | ratom，datascript |
| chan | flow state | channel | core.async.chan |
| subscribe | subscribe reaction | ref-> reaction | tree of reactions |
| view | view model | model -> reactions -> react component | reagent, rum |
| dispatch | dispatch event | event -> request | |
| tap | tap model | value->value | user-defined, for handle, pure function |
| handle！ | handle! request | request -> effect | default to db-handler |
| - inject | inject co-effect | request -> request | support for multiple co-fx |
| - handle | handle request | request -> response | db-handler, pure function |
| - do！ | do! effect | response -> effect | support for multiple fx |
| service | long-run for request | go-loop | support for sync and async |
| task | once/periodic | | e.g. init process | |

## Life cycle of machine

* Definition period: in fancoil.base, the type and interface of a machine is defined by defmulti
    ``` clojure
    (defmulti handle!
        "stateful function
        request in -> effects out
        config: inject, handle, doall!, other resources"
        (fn [config signal & rest] signal))
    ```
* Implementation period: in fancoil.plugin, some methods of base are implemented, you can include them. Or you can use defmethod to implement them in your project. Method may call other methods of same multi-fn, as is common in handle and subscribe.
  ``` clojure
  (defmethod base/handle! :default
    [{:keys [doall! handle inject]} signal req]
    (let [req (inject :ratom/db req)
          resp (handle signal req)]
      (doall! resp)))
  ```
* Runtime period: in fancoil.unit, some integrant init-key method is implemented, and integrant will inject the configuration into the machine when it initializes the system

  ``` clojure
  (defmethod ig/init-key ::handle!
     [_ config]
     (partial base/handle! config))        
  
  (def config 
    {::handle! {:handle (ig/ref ::handle)
                :inject (ig/ref ::inject)
               :doall! (ig/ref ::doall!)}
   ;; other config })
  ```

## Features
- Separation of state: stateful dependencies are injected until last minute
- More functions: more pure or formal functions
- Highly configurable:  flexibility to change the system structure via integrant config
- Highly extensible: extend for existing machines via lib/plugins. Or write new integrant modules.
- Easy to test: use integrant to init parts of the system to do unit tests on the machine

## How to use
- Read the source code: not much
- Read some examples: [fancoil-example], includes simple, todomvc (ratom/poshed datascript), catchat (frontend+backend)

[fancoil-example]:https://github.com/itarck/fancoil-example

## Credits and Thanks
- [@richhickey]:  multi-methods, a powerful runtime polymorphism
- [@weavejester]: [integrant], an elegant approach to system integration
- [@day8]: [re-frame], a clear frontend application architecture

[@richhickey]:https://github.com/richhickey
[@weavejester]:https://github.com/weavejester
[@day8]:https://github.com/day8
[re-frame]:https://github.com/day8/re-frame

## Other notes
- still pre-alpha, fancoil.module may change, or move to another repo
- request is hash-map, open. when injecting cofx, it will add namespaced key of the injector.
- response is hash-map, open. doall! can execute all effects, no guarantee of order. If you need to guarantee the order, use a vector of key-value pairs, or just use fx/doseq
