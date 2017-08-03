//
//  NameRecognizer.cpp
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 08/02/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#include "NameRecognizer.h"
#include "IServiceContainer.h"
#include "INeuralNetworkObjectFactory.h"
#include "Utils.h"
#include "IRecognitionCoreDelegate.h"
#include <iomanip>      // std::setprecision
#include <iterator>
#include <sstream>

using namespace std;
using namespace cv;

static const cv::Rect nameWindowRect(25,325,494,80);
static const cv::Size charSize(20,29);

static const std::vector<string> alphabet = {" ","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};

static const std::vector<string> namePrefixes = {"MR","MS","MRS"};

CNameRecognizer::CNameRecognizer(const shared_ptr<IServiceContainer>& container) : _container(container)
{
    if(auto container = _container.lock()) {
        _factory = container->resolve<INeuralNetworkObjectFactory>();
    }
}

CNameRecognizer::~CNameRecognizer()
{
}

void CNameRecognizer::SetRecognitionMode(PayCardsRecognizerMode flag)
{
    _mode = flag;
}

void CNameRecognizer::SetPathNameLocalizationXModel(const string& path)
{
    _pathNameLocalizationXModel = path;
}

void CNameRecognizer::SetPathNameLocalizationXStruct(const string& path)
{
    _pathNameLocalizationXStruct = path;
}

void CNameRecognizer::SetDelegate(const shared_ptr<IRecognitionCoreDelegate>& delegate)
{
    _delegate = delegate;
}

void CNameRecognizer::SetPathNameYLocalizationViola(const string& path)
{
    _pathNameYLocalizationViola = path;
}

void CNameRecognizer::SetPathNameSpaceCharModel(const string& path)
{
    _pathNameSpaceCharModel = path;
}

void CNameRecognizer::SetPathNameSpaceCharStruct(const string& path)
{
    _pathNameSpaceCharStruct = path;
}

void CNameRecognizer::SetPathNameListTxt(const string& path)
{
    _pathNameDictPath = path;
}

bool CNameRecognizer::Deploy()
{
    if(auto factory = _factory.lock()) {
    
        bool cascadeFlag = false;
        
        if (_pathNameYLocalizationViola.length() > 0) {
            cascadeFlag = _yCascade.load(_pathNameYLocalizationViola);
        }
        
        _localizationXNeuralNetwork = factory->CreateNeuralNetwork("", _pathNameLocalizationXStruct, _pathNameLocalizationXModel);
        
        _spaceCharNeuralNetwork = factory->CreateNeuralNetwork("", _pathNameSpaceCharStruct, _pathNameSpaceCharModel);
        
        if (_pathNameDictPath.length() > 0) {
            
            std::ifstream namesDict(_pathNameDictPath);
            string fileContents { istreambuf_iterator<char>(namesDict), istreambuf_iterator<char>() };
            
            _namesDict.str(fileContents);
        }
        
        return _localizationXNeuralNetwork->IsDeployed() &&
                _spaceCharNeuralNetwork->IsDeployed() && cascadeFlag && _pathNameDictPath.length() > 0;
    }
    
    return false;
}


shared_ptr<INeuralNetworkResultList> CNameRecognizer::Process(cv::Mat& matrix, vector<cv::Mat>& charSamples,
                                                              std::string& postprocessedName)
{
    vector<Rect> charRects;
    
    Mat nameMat = matrix(nameWindowRect);
    
    _yCascade.detectMultiScale(nameMat, charRects, 1.05, 3, 0|CV_HAAR_SCALE_IMAGE, cv::Size(charSize.width-1, charSize.height-1), cv::Size(charSize.width+1, charSize.height+1));

    Mat hist(nameWindowRect.height, 1, CV_8UC1, Scalar(0,0,0));
    
    for(Rect rect : charRects) {
        for (int row = rect.y; row<rect.y+rect.height; row++) {
            hist.at<uchar>(row, 0) += 1;
        }
    }
    
    int minY = -1, maxY = -1;
    int max = 0;
    
    for (int i = 0 ; i<nameWindowRect.height - charSize.height; i++) {
        
        Mat currentMat = hist(Rect(0,i,1,charSize.height));
        
        int current = cv::sum(currentMat)[0];
        
        if (max < current) {
            max = current;
            minY = i;
            maxY = -1;
        }
    }
    
    Rect roi = Rect(0, cvRound(minY), nameMat.cols, charSize.height+4);
    
    if (CUtils::ValidateROI(nameMat, roi)) {
        if(auto factory = _factory.lock()) {
            Mat xLoc = nameMat(roi);
            
            _yCascade.detectMultiScale(xLoc, charRects, 1.05, 3, 0|CV_HAAR_SCALE_IMAGE,
                                       cv::Size(charSize.width-1, charSize.height-1), cv::Size(charSize.width+1, charSize.height+1));
            
            vector<float> xCoord;
            
            for(Rect rect : charRects) {
                xCoord.push_back(float(rect.x));
            }

            if (xCoord.size() > 0) {

                int bestTotalScore = 0;
                int bestPeak = 0;
                int bestIndex = -1;
                float bestModelDist = 0.0;
                
                for (int i=0; i<10; i++) {
                    std::pair<int, int> result;
                    float modelDist = 19.4 + float(i)/20.0;
                    int totalScore = FilterCoords(xCoord, modelDist, result);
                    if (totalScore > bestTotalScore) {
                        bestTotalScore = totalScore;
                        bestIndex = result.first;
                        bestPeak = result.second;
                        bestModelDist = modelDist;
                    }
                    else if(totalScore == bestTotalScore && result.second > bestPeak) {
                        bestTotalScore = totalScore;
                        bestIndex = result.first;
                        bestPeak = result.second;
                        bestModelDist = modelDist;
                    }
                }

                cv::Rect doubleRect = cv::Rect(cvRound(xCoord[bestIndex]), 2, 44, 28);
                
                if (CUtils::ValidateROI(nameMat, doubleRect)) {
                    Mat doubleCharsMat = xLoc(doubleRect);
                    
                    shared_ptr<INeuralNetworkResultList> refineResult = factory->CreateNeuralNetworkResultList();
                    
                    Predict({doubleCharsMat}, refineResult, _localizationXNeuralNetwork);
                    
                    shared_ptr<INeuralNetworkResult> result = refineResult->GetAtIndex(0);
                    vector<pair<int, float>> data = result->GetRawResult();
                    float newBest = xCoord[bestIndex] + (data.at(0).second+data.at(1).second)*22.0; //(x0+x1)/2 * 44
                    
                    
                    vector<float> charRects;
                    
                    charRects.push_back(newBest);
                    
                    float floatingX = newBest - bestModelDist;
                    
                    while (floatingX >= 0) {
                        charRects.push_back(floatingX);
                        floatingX -= bestModelDist;
                    }
                    
                    floatingX = newBest + bestModelDist;
                    
                    while (floatingX + charSize.width < roi.width) {
                        charRects.push_back(floatingX);
                        floatingX += bestModelDist;
                    }
                    
                    sort(charRects.begin(), charRects.end(),
                         [](const float & a, const float & b) -> bool { return a < b;});
                    
                    vector<cv::Mat> chars;
                    int count = 0;
                    for (float x : charRects) {
                        
                        int roundedX = cvRound(x);
                        chars.push_back(xLoc(cv::Rect(roundedX, 1, 20, 29)));
                        count++;

                        if (count == 22) break;
                    }
                    
                    shared_ptr<INeuralNetworkResultList> charResult = factory->CreateNeuralNetworkResultList();
                    
                    Predict(chars, charResult, _spaceCharNeuralNetwork);
                    int charsCount = 0;
                    
                    /// check probabilities
                    const float threshold = 0.95;
                    const int maxDoubtfulCount = 2;
                    
                    int non = 0;
                    
                    string resultStr = "";
                    
                    string resultStr0 = "";
                    ostringstream resultStrProb0;

                    string resultStr1 = "";
                    ostringstream resultStrProb1;
                    
                    for (INeuralNetworkResultList::ResultIterator it = charResult->Begin(); it != charResult->End(); ++it) {
                        
                        shared_ptr<INeuralNetworkResult> resultItem = *it;
                        int charIdx = resultItem->GetMaxIndex();
                        
                        if (charIdx > 0) charsCount++;
                        
                        shared_ptr<INeuralNetworkResult> result = *it;
                        
                        if (result->GetMaxProbability() < threshold) {
                            non++;
                        }

                        resultStr += alphabet[charIdx];

                
                        resultStr0 += "  " + alphabet[charIdx] + "  ";
                        
                        int secondIndex;
                        float probabilityDiff;

                        resultItem->GetSecondValue(secondIndex, probabilityDiff);
                        resultStr1 += "  " + alphabet[secondIndex] + "  ";
                        
                        resultStrProb0 << fixed << setprecision(2) << resultItem->GetMaxProbability();
                        resultStrProb0 << ";";
                        
                        resultStrProb1 << fixed << setprecision(2) << probabilityDiff;
                        resultStrProb1 << ";";
                    }
                    
                    if (charsCount > 5 && non < maxDoubtfulCount) {
                     
                        resultStr.erase(resultStr.begin(), std::find_if(resultStr.begin(), resultStr.end(), std::not1(std::ptr_fun<int, int>(std::isspace))));
                        resultStr.erase(std::find_if(resultStr.rbegin(), resultStr.rend(), std::not1(std::ptr_fun<int, int>(std::isspace))).base(), resultStr.end());

                        istringstream iss(resultStr);
                        
                        vector<string> chunks{istream_iterator<string>{iss},
                            istream_iterator<string>{}};
                        
                        // check for prefix: MS, MR, MRS
                        if (chunks.size() > 0 && chunks[0].size() <= 3 && chunks[0].size() >= 2) {
                            
                            int minSimilarity = INT_MAX;
                            string minChunk;
                            
                            for (string prefix : namePrefixes) {
                                
                                int currSimilarity = LevenshteinDistance(prefix, chunks[0]);
                                
                                if (currSimilarity < minSimilarity) {
                                    minSimilarity = currSimilarity;
                                    minChunk = prefix;
                                }
                            }
                            
                            if (minSimilarity == 1) {
                                chunks[0] = minChunk;
                            }
                        }
                        
                        for (string& chunk : chunks) {
                            
                            string dictName;

                            int minSimilarity = INT_MAX;
                            string minChunk;
                            _namesDict.clear();
                            _namesDict.seekg(0, ios::beg);
                            
                            while (_namesDict >> dictName) {
                                
                                int currSimilarity = LevenshteinDistance(dictName, chunk);
                                
                                if (currSimilarity < minSimilarity) {
                                    minSimilarity = currSimilarity;
                                    minChunk = dictName;
                                }
                            }
                            
                            if (minSimilarity != 0 && minSimilarity < 2 && minChunk.size() == chunk.size()) {
                                chunk = minChunk;
                            }
                        }
                        
                        std::stringstream ss;
                        const int chunkSize = (int)chunks.size();
                        for(int i=0; i<chunkSize; ++i) {
                            if(i != 0) ss << " ";
                            ss << chunks[i];
                        }
                        
                        postprocessedName = ss.str();
                        
                        return charResult;
                    }
                }
            }
        }
    }
    
    return nullptr;
}

int CNameRecognizer::FilterCoords(const std::vector<float>& xCoords,
                                  float modelDist, std::pair<int, int>& result)
{
    std::vector<int> resultIdxs;

    const float threshold = 1.0;
    
    std::map<int, int> coordScore;
    
    int totalScore = 0;
    
    for (int i=0; i<xCoords.size(); i++) {
        
        coordScore.insert(std::make_pair(i, 0));
        
        for (int j=0; j<xCoords.size(); j++) {
            float dist = abs(xCoords[i]-xCoords[j]);
            float remainder = abs(dist - cvRound(dist/modelDist)*modelDist);
            
            if (remainder <= threshold) {
                coordScore[i] += 1;
                totalScore++;
            }
        }
    }
    
    int maxScore = 0;
    for(auto const &pair : coordScore) {
        
        if (maxScore < pair.second) {
            maxScore = pair.second;
            result.second = maxScore;
            result.first = pair.first;
        }
    }
    
    return totalScore;
}

void CNameRecognizer::Predict(const vector<Mat>& matrixes, shared_ptr<INeuralNetworkResultList>& neuralNetworkResultList, const shared_ptr<INeuralNetwork>& neuralNetwork)
{
    if(auto factory = _factory.lock()) {
        shared_ptr<INeuralNetworkDatumList> neuralNetworkDatumList = factory->CreateNeuralNetworkDatumList();
        
        for(Mat matrix : matrixes) {
            shared_ptr<INeuralNetworkDatum> neuralNetworkDatum = factory->CreateNeuralNetworkDatum(matrix);
            neuralNetworkDatumList->PushBack(neuralNetworkDatum);
        }
        
        neuralNetwork->Predict(neuralNetworkDatumList, neuralNetworkResultList);
    }
}


int CNameRecognizer::LevenshteinDistance(const string& src, const string& dst)
{
    const int m = (int)src.size();
    const int n = (int)dst.size();
    if (m == 0) {
        return n;
    }
    if (n == 0) {
        return m;
    }
    
    std::vector< std::vector<int> > matrix(m + 1);
    
    for (int i = 0; i <= m; ++i) {
        matrix[i].resize(n + 1);
        matrix[i][0] = i;
    }
    for (int i = 0; i <= n; ++i) {
        matrix[0][i] = i;
    }
    
    int above_cell, left_cell, diagonal_cell, cost;
    
    for (int i = 1; i <= m; ++i) {
        for(int j = 1; j <= n; ++j) {
            cost = src[i - 1] == dst[j - 1] ? 0 : 1;
            above_cell = matrix[i - 1][j];
            left_cell = matrix[i][j - 1];
            diagonal_cell = matrix[i - 1][j - 1];
            matrix[i][j] = std::min(std::min(above_cell + 1, left_cell + 1), diagonal_cell + cost);
        }
    }
    
    return matrix[m][n];
}
