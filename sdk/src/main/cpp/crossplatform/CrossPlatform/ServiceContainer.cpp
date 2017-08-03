//
//  ServiceContainer.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 09/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#include "ServiceContainer.h"
#include "CaffeObjectFactory.h"
#include "EdgesDetector.h"
#include "FrameStorage.h"
#include "RecognitionResult.h"
#include "DateRecognizer.h"
#include "NumberRecognizer.h"
#include "TorchManager.h"
#include "NameRecognizer.h"

#include "IEdgesDetector.h"
#include "IFrameStorage.h"
#include "IRecognitionResult.h"
#include "IDateRecognizer.h"
#include "INumberRecognizer.h"
#include "ITorchManager.h"
#include "INameRecognizer.h"

// Static factory implementation
bool IServiceContainerFactory::CreateServiceContainer(shared_ptr<IServiceContainer> &serviceContainer)
{
    serviceContainer = make_shared<CServiceContainer>();
    return serviceContainer != 0;
}

// Service container implementation
CServiceContainer::CServiceContainer()
{
}

CServiceContainer::~CServiceContainer( void )
{
    _serviceGenerators.clear();
    _singleInstances.clear();
}

bool CServiceContainer::Initialize()
{
    shared_ptr<IServiceContainer> sharedThis = shared_from_this();
    
    shared_ptr<IObjectFactory> objectFactory = make_shared<IObjectFactory>();
    mapSingleInstanceTypeToGenerator<IObjectFactory>(objectFactory);
    
    shared_ptr<CCaffeObjectFactory> caffeObjectFactory = make_shared<CCaffeObjectFactory>(sharedThis);
    mapSingleInstanceTypeToGenerator<INeuralNetworkObjectFactory>(caffeObjectFactory);
    
    shared_ptr<CFrameStorage> frameStorageObject = make_shared<CFrameStorage>();
    mapSingleInstanceTypeToGenerator<IFrameStorage>(frameStorageObject);
    
    shared_ptr<CEdgesDetector> edgesDetectorObject = make_shared<CEdgesDetector>(frameStorageObject);
    mapSingleInstanceTypeToGenerator<IEdgesDetector>(edgesDetectorObject);
    
    shared_ptr<CRecognitionResult> recognitionResultObject = make_shared<CRecognitionResult>();
    mapSingleInstanceTypeToGenerator<IRecognitionResult>(recognitionResultObject);
    
    shared_ptr<CNumberRecognizer> numberRecognizerObject = make_shared<CNumberRecognizer>(sharedThis);
    mapSingleInstanceTypeToGenerator<INumberRecognizer>(numberRecognizerObject);
    
    shared_ptr<CTorchManager> torchManagerObject = make_shared<CTorchManager>();
    mapSingleInstanceTypeToGenerator<ITorchManager>(torchManagerObject);
    
    shared_ptr<CDateRecognizer> dateRecognizerObject = make_shared<CDateRecognizer>(sharedThis);
    mapSingleInstanceTypeToGenerator<IDateRecognizer>(dateRecognizerObject);
    
    shared_ptr<CNameRecognizer> nameRecognizerObject = make_shared<CNameRecognizer>(sharedThis);
    mapSingleInstanceTypeToGenerator<INameRecognizer>(nameRecognizerObject);
    
    return true;
}

shared_ptr<IBaseObj> CServiceContainer::resolve( const type_info &service )
{
    shared_ptr<IBaseObj> ptr = nullptr;
    
    ServiceGenerators::iterator it = _serviceGenerators.find(type_index(service));
    if(it != _serviceGenerators.end()) {
        ptr = it->second(shared_from_this());
    }
    
    return ptr;
}

shared_ptr<IBaseObj> CServiceContainer::getSingleInstance(const type_info &instance)
{
    shared_ptr<IBaseObj> ptr;
    
    SingleInstances::iterator it = _singleInstances.find(type_index(instance));
    
    if(it != _singleInstances.end()) {
        ptr = it->second;
    }
    
    return ptr;
}
