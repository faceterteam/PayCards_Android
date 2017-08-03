//
//  CaffeDatumList.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/06/15.
//  Copyright (c) 2015 Vladimir Tchernitski. All rights reserved.
//

#include "CaffeDatumList.h"
#include "CaffeDatum.h"

void CCaffeDatumList::PushBack(const shared_ptr<INeuralNetworkDatum>& datum)
{
    shared_ptr<CCaffeDatum> datumImplPtr = dynamic_pointer_cast<CCaffeDatum>(datum);
    
    _blobs.push_back(datumImplPtr->GetBlob());
}

void CCaffeDatumList::Clear()
{
    _blobs.clear();
}


int CCaffeDatumList::Size() const
{
    return (int)_blobs.size();
}

vector<cv::Mat> CCaffeDatumList::GetBlobs() const
{
    return _blobs;
}
