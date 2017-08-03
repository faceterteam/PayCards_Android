#include <vector>

#include "caffe/filler.hpp"
#include "caffe/layer.hpp"
#include "caffe/util/im2col.hpp"
#include "caffe/util/math_functions.hpp"
#include "caffe/vision_layers.hpp"

namespace caffe {

template <typename Dtype>
void ConvolutionLayer<Dtype>::compute_output_shape() {
  this->height_out_ = (this->height_ + 2 * this->pad_h_ - this->kernel_h_)
      / this->stride_h_ + 1;
  this->width_out_ = (this->width_ + 2 * this->pad_w_ - this->kernel_w_)
      / this->stride_w_ + 1;
}

template <typename Dtype>
void ConvolutionLayer<Dtype>::Forward_cpu(const vector<Blob<Dtype>*>& bottom,
      const vector<Blob<Dtype>*>& top) {
  if(this->NTILE_WIDTH_ * this->NTILE_HEIGHT_ <= 1){
  const Dtype* weight = this->blobs_[0]->cpu_data();
  for (int i = 0; i < bottom.size(); ++i) {
    const Dtype* bottom_data = bottom[i]->cpu_data();
    Dtype* top_data = top[i]->mutable_cpu_data();
    for (int n = 0; n < this->num_; ++n) {
      this->forward_cpu_gemm(bottom_data + bottom[i]->offset(n), weight,
          top_data + top[i]->offset(n));
      if (this->bias_term_) {
        const Dtype* bias = this->blobs_[1]->cpu_data();
        this->forward_cpu_bias(top_data + top[i]->offset(n), bias);
      }
    }
  }
  }else{
	  CHECK_EQ(this->stride_h_, 1);
	  CHECK_EQ(this->stride_w_, 1);
	  CHECK_EQ(this->pad_h_, 0);
	  CHECK_EQ(this->pad_w_, 0);
	  CHECK_EQ(this->group_, 1);
	  CHECK_EQ(this->kernel_h_, this->kernel_w_);
	  CHECK_EQ(this->col_buffer_.height(), this->TILE_HEIGHT_);
	  CHECK_EQ(bottom.size(), 1);
          const Dtype* bottom_data = bottom[0]->cpu_data();
	  Dtype* top_data = top[0]->mutable_cpu_data();
          Dtype* col_data = this->col_buffer_.mutable_cpu_data();
	  Dtype *out_buffer = this->out_buffer_.mutable_cpu_data();
	  for (int n = 0; n < this->num_; ++n) {
		  for(int ny = 0; ny < this->NTILE_HEIGHT_; ny++){
			  for(int nx = 0; nx < this->NTILE_WIDTH_; nx++){
				  int idx = ny * this->NTILE_WIDTH_ + nx;
				  const Dtype* weight = this->blobs_[idx]->cpu_data();
				  const Dtype * img = bottom_data + bottom[0]->offset(n, 0,
						  this->TILE_HEIGHT_ * ny, this->TILE_WIDTH_ * nx);
				  im2col_tile_cpu(img,   this->channels_, this->height_,
						  this->width_, this->kernel_h_, col_data,
						  this->TILE_HEIGHT_, this->TILE_WIDTH_);
				  //dump(&col_buffer_);
				  int M_ = this->num_output_ / this->group_;
				  int K_ = this->channels_ * this->kernel_h_ * this->kernel_w_ / this->group_;
				  int N_ = this->TILE_WIDTH_ * this->TILE_HEIGHT_;
				  caffe_cpu_gemm<Dtype>(CblasNoTrans, CblasNoTrans, M_, N_, K_,
						  (Dtype)1., weight, col_data, (Dtype)0., out_buffer);

				  if (this->bias_term_) {
					  const Dtype *bias_ptr = this->blobs_[idx + this->NTILE_WIDTH_ *
						  this->NTILE_HEIGHT_]->cpu_data();
					  caffe_cpu_gemm<Dtype>(CblasNoTrans, CblasNoTrans, this->num_output_,
							  N_, 1, (Dtype)1., bias_ptr,
							  reinterpret_cast<const Dtype*>(this->bias_multiplier_.cpu_data()),
							  (Dtype)1., out_buffer);
				  }
				  //dump(&out_buffer_);
				  /* copy back */

				  int height_out = this->height_ - this->kernel_h_ + 1;
				  int width_out = this->width_ - this->kernel_w_ + 1;
				  copy_stride_cpu(out_buffer, this->num_output_, this->TILE_HEIGHT_, this->TILE_WIDTH_,
						  top_data + top[0]->offset(n, 0, this->TILE_HEIGHT_*ny,
							  this->TILE_WIDTH_*nx), height_out, width_out);

			  }

		      }
		  }
  }
}

template <typename Dtype>
void ConvolutionLayer<Dtype>::Backward_cpu(const vector<Blob<Dtype>*>& top,
      const vector<bool>& propagate_down, const vector<Blob<Dtype>*>& bottom) {
//  const Dtype* weight = this->blobs_[0]->cpu_data();
//  Dtype* weight_diff = this->blobs_[0]->mutable_cpu_diff();
//  if (this->param_propagate_down_[0]) {
//    caffe_set(this->blobs_[0]->count(), Dtype(0), weight_diff);
//  }
//  if (this->bias_term_ && this->param_propagate_down_[1]) {
//    caffe_set(this->blobs_[1]->count(), Dtype(0),
//        this->blobs_[1]->mutable_cpu_diff());
//  }
//  for (int i = 0; i < top.size(); ++i) {
//    const Dtype* top_diff = top[i]->cpu_diff();
//    const Dtype* bottom_data = bottom[i]->cpu_data();
//    Dtype* bottom_diff = bottom[i]->mutable_cpu_diff();
//    // Bias gradient, if necessary.
//    if (this->bias_term_ && this->param_propagate_down_[1]) {
//      Dtype* bias_diff = this->blobs_[1]->mutable_cpu_diff();
//      for (int n = 0; n < this->num_; ++n) {
//        this->backward_cpu_bias(bias_diff, top_diff + top[i]->offset(n));
//      }
//    }
//    if (this->param_propagate_down_[0] || propagate_down[i]) {
//      for (int n = 0; n < this->num_; ++n) {
//        // gradient w.r.t. weight. Note that we will accumulate diffs.
//        if (this->param_propagate_down_[0]) {
//          this->weight_cpu_gemm(bottom_data + bottom[i]->offset(n),
//              top_diff + top[i]->offset(n), weight_diff);
//        }
//        // gradient w.r.t. bottom data, if necessary.
//        if (propagate_down[i]) {
//          this->backward_cpu_gemm(top_diff + top[i]->offset(n), weight,
//              bottom_diff + bottom[i]->offset(n));
//        }
//      }
//    }
//  }
}

#ifdef CPU_ONLY
STUB_GPU(ConvolutionLayer);
#endif

INSTANTIATE_CLASS(ConvolutionLayer);

}  // namespace caffe
