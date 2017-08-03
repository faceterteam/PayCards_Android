#ifndef _CAFFE_UTIL_IM2COL_HPP_
#define _CAFFE_UTIL_IM2COL_HPP_

namespace caffe {

template <typename Dtype>
void im2col_cpu(const Dtype* data_im, const int channels,
    const int height, const int width, const int kernel_h, const int kernel_w,
    const int pad_h, const int pad_w, const int stride_h,
    const int stride_w, Dtype* data_col);

template <typename Dtype>
void col2im_cpu(const Dtype* data_col, const int channels,
    const int height, const int width, const int patch_h, const int patch_w,
    const int pad_h, const int pad_w, const int stride_h,
    const int stride_w, Dtype* data_im);

template <typename Dtype>
void im2col_gpu(const Dtype* data_im, const int channels,
    const int height, const int width, const int kernel_h, const int kernel_w,
    const int pad_h, const int pad_w, const int stride_h,
    const int stride_w, Dtype* data_col);

template <typename Dtype>
void col2im_gpu(const Dtype* data_col, const int channels,
    const int height, const int width, const int patch_h, const int patch_w,
    const int pad_h, const int pad_w, const int stride_h,
    const int stride_w, Dtype* data_im);

template <typename Dtype>
void im2col_tile_cpu(const Dtype* data_im, const int channels,
		const int stride_h, const int stride_w,
    const int ksize, Dtype* data_col, 
    const int height_col, const int width_col);

template <typename Dtype>
void copy_stride_cpu(const Dtype* src_data, 
		const int channels,
		const int height, const int width, Dtype *dst_data, 
		const int stride_h, const int stride_w);


}  // namespace caffe

#endif  // CAFFE_UTIL_IM2COL_HPP_
