//
//  ServiceContainer.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#ifndef __CardRecognizer__ServiceContainer__
#define __CardRecognizer__ServiceContainer__

#include <typeindex>

#include "IObjectFactory.h"
#include "IServiceContainer.h"

using namespace std;

class CServiceContainer: public IServiceContainer
{
public:
    
    CServiceContainer();
    
    virtual ~CServiceContainer(void);
    
public:
    
    virtual bool Initialize();
    
    virtual shared_ptr<IBaseObj> resolve(const type_info& service);
    
public:
    
    virtual shared_ptr<IBaseObj>	getSingleInstance(const type_info &instance);
    
protected:
    typedef map<type_index, shared_ptr<IBaseObj>>  SingleInstances;

    typedef function<shared_ptr<IBaseObj>(shared_ptr<IServiceContainer> container)>	ServiceGenerator;
    typedef map<std::type_index, ServiceGenerator>				ServiceGenerators;
    
    template <class T>
    bool mapTypeToGenerator(ServiceGenerator generateType);
    
    
    // Maps a single instance to its type.
    template <class T>
    bool mapSingleInstanceTypeToGenerator(shared_ptr<T> singleInstance);
    
    
    // Maps single instance to a custom type, meaning not the actual type of the instance(!).
    // This is used for having 2 "single" instances of the same type, for different purposes.
    template <class customT, class T>
    bool mapSingleInstanceCustomTypeToGenerator(shared_ptr<T> singleInstance);
    
    // Performs the actual registration of an instance against the type provided (not necessarily the instance's actual type)
    template <class T>
    bool mapSingleInstanceTypeToGenerator(const type_info &registrationType, shared_ptr<T> singleInstance);
    
protected:
    
    ServiceGenerators	_serviceGenerators;
    SingleInstances		_singleInstances;
};

template <class T>
bool CServiceContainer::mapTypeToGenerator(ServiceGenerator generateType)
{
    /// Map the type name (string) to the "generator" function object
    return (_serviceGenerators.insert(pair<std::type_index, ServiceGenerator>(std::type_index(typeid(T)), generateType)).second == true);
}

template <class T>
bool CServiceContainer::mapSingleInstanceTypeToGenerator(shared_ptr<T> singleInstance)
{
    return mapSingleInstanceTypeToGenerator<T>(typeid(T), singleInstance);
}
//
template <class customT, class T>
bool CServiceContainer::mapSingleInstanceCustomTypeToGenerator(shared_ptr<T> singleInstance)
{
    return mapSingleInstanceTypeToGenerator<T>(typeid(customT), singleInstance);
}

template <class T>
bool CServiceContainer::mapSingleInstanceTypeToGenerator(const type_info &registrationType, shared_ptr<T> singleInstance)
{
    // Check if a type of this instance already exist and add to the single instances map if not
    SingleInstances::iterator it = _singleInstances.find(type_index(registrationType));
    if (it == _singleInstances.end())
    {
        _singleInstances.insert(make_pair(type_index(registrationType), dynamic_pointer_cast<IBaseObj>(singleInstance)));
    }
    
    // Create the "generator" function object
    ServiceGenerator generateType = [&registrationType](shared_ptr<IServiceContainer> container)->shared_ptr<IBaseObj> {
        shared_ptr<IBaseObj> singleInstance;
        shared_ptr<CServiceContainer> concreteContainer = static_pointer_cast<CServiceContainer>(container);
        
        if (concreteContainer != 0)
        {
            singleInstance = concreteContainer->getSingleInstance(registrationType);
        }
        return singleInstance;
    };
    
    // Map the type name (string) to the "generator" function object
    return (_serviceGenerators.insert(std::make_pair(std::type_index(registrationType), generateType)).second == true);
}

#endif /* defined(__CardRecognizer__ServiceContainer__) */
