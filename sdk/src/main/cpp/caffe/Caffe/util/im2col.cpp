// Copyright 2013 Yangqing Jia

#include <cmath>
#include <cstdlib>
#include <cstring>

#include "caffe/util/im2col.hpp"
#include "caffe/util/math_functions.hpp"

namespace caffe {
template <typename Dtype>
void im2col_cpu(const Dtype* data_im, const int channels,
    const int height, const int width, const int kernel_h, const int kernel_w,
    const int pad_h, const int pad_w,
    const int stride_h, const int stride_w,
    Dtype* data_col) {
  int height_col = (height + 2 * pad_h - kernel_h) / stride_h + 1;
  int width_col = (width + 2 * pad_w - kernel_w) / stride_w + 1;
  int channels_col = channels * kernel_h * kernel_w;
  for (int c = 0; c < channels_col; ++c) {
    int w_offset = c % kernel_w;
    int h_offset = (c / kernel_w) % kernel_h;
    int c_im = c / kernel_h / kernel_w;
    for (int h = 0; h < height_col; ++h) {
      for (int w = 0; w < width_col; ++w) {
        int h_pad = h * stride_h - pad_h + h_offset;
        int w_pad = w * stride_w - pad_w + w_offset;
        if (h_pad >= 0 && h_pad < height && w_pad >= 0 && w_pad < width)
          data_col[(c * height_col + h) * width_col + w] =
            data_im[(c_im * height + h_pad) * width + w_pad];
        else
          data_col[(c * height_col + h) * width_col + w] = 0;
      }
    }
  }
}

// Explicit instantiation
template void im2col_cpu<float>(const float* data_im, const int channels,
    const int height, const int width, const int kernel_h, const int kernel_w,
    const int pad_h, const int pad_w, const int stride_h,
    const int stride_w, float* data_col);
template void im2col_cpu<double>(const double* data_im, const int channels,
    const int height, const int width, const int kernel_h, const int kernel_w,
    const int pad_h, const int pad_w, const int stride_h,
    const int stride_w, double* data_col);

template <typename Dtype>
void col2im_cpu(const Dtype* data_col, const int channels,
    const int height, const int width, const int patch_h, const int patch_w,
    const int pad_h, const int pad_w,
    const int stride_h, const int stride_w,
    Dtype* data_im) {
  caffe_set(height * width * channels, Dtype(0), data_im);
  int height_col = (height + 2 * pad_h - patch_h) / stride_h + 1;
  int width_col = (width + 2 * pad_w - patch_w) / stride_w + 1;
  int channels_col = channels * patch_h * patch_w;
  for (int c = 0; c < channels_col; ++c) {
    int w_offset = c % patch_w;
    int h_offset = (c / patch_w) % patch_h;
    int c_im = c / patch_h / patch_w;
    for (int h = 0; h < height_col; ++h) {
      for (int w = 0; w < width_col; ++w) {
        int h_pad = h * stride_h - pad_h + h_offset;
        int w_pad = w * stride_w - pad_w + w_offset;
        if (h_pad >= 0 && h_pad < height && w_pad >= 0 && w_pad < width)
          data_im[(c_im * height + h_pad) * width + w_pad] +=
              data_col[(c * height_col + h) * width_col + w];
      }
    }
  }
}

// Explicit instantiation
template void col2im_cpu<float>(const float* data_col, const int channels,
    const int height, const int width, const int patch_h, const int patch_w,
    const int pad_h, const int pad_w, const int stride_h,
    const int stride_w, float* data_im);
template void col2im_cpu<double>(const double* data_col, const int channels,
    const int height, const int width, const int patch_h, const int patch_w,
    const int pad_h, const int pad_w, const int stride_h,
    const int stride_w, double* data_im);



template <typename Dtype>
static void im2col_tile_cpu_kernel(const int n, const Dtype* _data_im,
    const int strideh, const int stridew, 
    const int ksize, 
    const int height_col, const int width_col,
    Dtype* _data_col) {
	for(int _index = 0; _index < n; _index++){
		int index = _index;
		int w_out = index % width_col;
		index /= width_col;
		int h_out = index % height_col;
		int channel_in = index / height_col;
		int channel_out = channel_in * ksize * ksize;
		int h_in = h_out;
		int w_in = w_out;
		Dtype * data_col = _data_col + (channel_out * height_col + h_out) * width_col + w_out;
		const Dtype *data_im = _data_im + (channel_in * strideh + h_in) * stridew + w_in;
		for (int i = 0; i < ksize; ++i) {
			for (int j = 0; j < ksize; ++j) {
				*data_col = data_im[i * stridew + j];
				data_col += height_col * width_col;
			}
		}
	}
}

template <typename Dtype>
void im2col_tile_cpu(const Dtype* data_im, const int channels,
		const int stride_h, const int stride_w,
    const int ksize, Dtype* data_col, 
    const int height_col, const int width_col) {
  // We are going to launch channels * height_col * width_col kernels, each
  // kernel responsible for copying a single-channel grid.
  int num_kernels = channels * height_col * width_col;
  // NOLINT_NEXT_LINE(whitespace/operators)
  im2col_tile_cpu_kernel<Dtype>(num_kernels, data_im, stride_h, stride_w, ksize, height_col,
      width_col, data_col);
}

template <typename Dtype>
static void copy_stride_cpu_kernel(int n, const Dtype* _src_data, 
		const int channels,
		const int height, const int width, Dtype *_dst_data, 
		const int stride_h, const int stride_w) {
#if 0
  for(int index = 0; index < n; index++){
    int w = index % width;
    int h = (index / width) % height;
    int c = index / (width * height);
    
    const Dtype * src_data = _src_data + (c * height + h) * width + w;
    Dtype * dst_data = _dst_data + (c * stride_h + h) * stride_w + w;
    *dst_data = *src_data;
  }
#endif
  for(int c = 0; c < channels; c++){
	  Dtype *pd = _dst_data + c * stride_h * stride_w;
	  for(int h = 0; h < height; h++){
		  for(int w = 0; w < width; w++){
			  pd[w] = *_src_data++;
		  }
		  pd += stride_w;
	  }
  }
}

template <typename Dtype>
void copy_stride_cpu(const Dtype* src_data, 
		const int channels,
		const int height, const int width, Dtype *dst_data, 
		const int stride_h, const int stride_w) {
  int num_kernels = channels * height * width;
  // To avoid involving atomic operations, we will launch one kernel per
  // bottom dimension, and then in the kernel add up the top dimensions.
  // NOLINT_NEXT_LINE(whitespace/operators)
  copy_stride_cpu_kernel<Dtype>(
      num_kernels, src_data, channels, height, width,
      dst_data, stride_h, stride_w);
}

// Explicit instantiation

template void im2col_tile_cpu(const float* data_im, const int channels,
		const int stride_h, const int stride_w,
    const int ksize, float* data_col, 
    const int height_col, const int width_col);
template void im2col_tile_cpu(const double* data_im, const int channels,
		const int stride_h, const int stride_w,
    const int ksize, double* data_col, 
    const int height_col, const int width_col);

template void copy_stride_cpu<float>(const float* src_data, 
		const int channels,
		const int height, const int width, float *dst_data, 
		const int stride_h, const int stride_w) ;
template void copy_stride_cpu<double>(const double* src_data, 
		const int channels,
		const int height, const int width, double *dst_data, 
		const int stride_h, const int stride_w) ;


}  // namespace caffe
