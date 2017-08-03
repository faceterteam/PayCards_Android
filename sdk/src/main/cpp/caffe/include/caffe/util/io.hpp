// Copyright Yangqing Jia 2013

#ifndef CAFFE_UTIL_IO_H_
#define CAFFE_UTIL_IO_H_

#include <string>

#include "google/protobuf/message.h"
#include "caffe/proto/caffe.pb.h"

#include "caffe/blob.hpp"

using std::string;
using ::google::protobuf::Message;

namespace caffe {

bool ReadProtoFromTextFile(const char* filename,
    Message* proto);
    
bool ReadProtoFromString(const string& input,
                         Message* proto);
    
inline bool ReadProtoFromTextFile(const string& filename,
    Message* proto) {
  return ReadProtoFromTextFile(filename.c_str(), proto);
}

void WriteProtoToTextFile(const Message& proto, const char* filename);
inline void WriteProtoToTextFile(const Message& proto, const string& filename) {
  WriteProtoToTextFile(proto, filename.c_str());
}

bool ReadProtoFromBinaryFile(const char* filename,
    Message* proto);
inline bool ReadProtoFromBinaryFile(const string& filename,
    Message* proto) {
  return ReadProtoFromBinaryFile(filename.c_str(), proto);
}

void WriteProtoToBinaryFile(const Message& proto, const char* filename);
inline void WriteProtoToBinaryFile(
    const Message& proto, const string& filename) {
  WriteProtoToBinaryFile(proto, filename.c_str());
}

bool ReadImageToDatum(const string& filename, const int label,
    const int height, const int width, Datum* datum);

inline bool ReadImageToDatum(const string& filename, const int label,
    Datum* datum) {
  return ReadImageToDatum(filename, label, 0, 0, datum);
}

void ReadNetParamsFromTextFileOrDie(const string& param_file,
                                    NetParameter* param);

void ReadNetParamsFromBinaryFileOrDie(const string& param_file,
                                      NetParameter* param);
#if 0
template <typename Dtype>
void hdf5_load_nd_dataset_helper(
  hid_t file_id, const char* dataset_name_, int min_dim, int max_dim,
  Blob<Dtype>* blob);

template <typename Dtype>
void hdf5_load_nd_dataset(
  hid_t file_id, const char* dataset_name_, int min_dim, int max_dim,
  Blob<Dtype>* blob);
#endif

}  // namespace caffe

#endif   // CAFFE_UTIL_IO_H_
