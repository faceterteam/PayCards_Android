// Copyright 2013 Yangqing Jia

#include "caffe/common.hpp"
#include "caffe/util/math_functions.hpp"

namespace caffe {

template<>
void caffe_cpu_gemm<float>(const CBLAS_TRANSPOSE TransA,
    const CBLAS_TRANSPOSE TransB, const int M, const int N, const int K,
    const float alpha, const float* A, const float* B, const float beta,
    float* C) {
#ifdef USE_EIGEN
	MAP_SMATRIX(eC, C, M, N);
	eC *= beta;
	if(TransA == CblasNoTrans && TransB == CblasNoTrans){
		MAP_CONST_SMATRIX(eA, A, M, K);
		MAP_CONST_SMATRIX(eB, B, K, N);
		eC.noalias() += alpha * (eA * eB);
	}else if(TransA == CblasNoTrans && TransB == CblasTrans){
		MAP_CONST_SMATRIX(eA, A, M, K);
		MAP_CONST_SMATRIX(eB, B, N, K);
		eC.noalias() += alpha * (eA * eB.transpose());
	}else if(TransA == CblasTrans && TransB == CblasNoTrans){
		MAP_CONST_SMATRIX(eA, A, K, M);
		MAP_CONST_SMATRIX(eB, B, K, N);
		eC.noalias() += alpha * (eA.transpose() * eB);
	}else{
		MAP_CONST_SMATRIX(eA, A, K, M);
		MAP_CONST_SMATRIX(eB, B, N, K);
		eC.noalias() += alpha * (eA.transpose() * eB.transpose());
	}
#else
  int lda = (TransA == CblasNoTrans) ? K : M;
  int ldb = (TransB == CblasNoTrans) ? N : K;
  cblas_sgemm(CblasRowMajor, TransA, TransB, M, N, K, alpha, A, lda, B,
      ldb, beta, C, N);
#endif
}

template<>
void caffe_cpu_gemm<double>(const CBLAS_TRANSPOSE TransA,
    const CBLAS_TRANSPOSE TransB, const int M, const int N, const int K,
    const double alpha, const double* A, const double* B, const double beta,
    double* C) {
#ifdef USE_EIGEN
	MAP_DMATRIX(eC, C, M, N);
	eC *= beta;
	if(TransA == CblasNoTrans && TransB == CblasNoTrans){
		MAP_CONST_DMATRIX(eA, A, M, K);
		MAP_CONST_DMATRIX(eB, B, K, N);
		eC.noalias() += alpha * (eA * eB);
	}else if(TransA == CblasNoTrans && TransB == CblasTrans){
		MAP_CONST_DMATRIX(eA, A, M, K);
		MAP_CONST_DMATRIX(eB, B, N, K);
		eC.noalias() += alpha * (eA * eB.transpose());
	}else if(TransA == CblasTrans && TransB == CblasNoTrans){
		MAP_CONST_DMATRIX(eA, A, K, M);
		MAP_CONST_DMATRIX(eB, B, K, N);
		eC.noalias() += alpha * (eA.transpose() * eB);
	}else{
		MAP_CONST_DMATRIX(eA, A, K, M);
		MAP_CONST_DMATRIX(eB, B, N, K);
		eC.noalias() += alpha * (eA.transpose() * eB.transpose());
	}
#else
  int lda = (TransA == CblasNoTrans) ? K : M;
  int ldb = (TransB == CblasNoTrans) ? N : K;
  cblas_dgemm(CblasRowMajor, TransA, TransB, M, N, K, alpha, A, lda, B,
      ldb, beta, C, N);
#endif
}

#if 0
template <>
void caffe_gpu_gemm<float>(const CBLAS_TRANSPOSE TransA,
    const CBLAS_TRANSPOSE TransB, const int M, const int N, const int K,
    const float alpha, const float* A, const float* B, const float beta,
    float* C) {
  // Note that cublas follows fortran order.
  int lda = (TransA == CblasNoTrans) ? K : M;
  int ldb = (TransB == CblasNoTrans) ? N : K;
  cublasOperation_t cuTransA =
      (TransA == CblasNoTrans) ? CUBLAS_OP_N : CUBLAS_OP_T;
  cublasOperation_t cuTransB =
      (TransB == CblasNoTrans) ? CUBLAS_OP_N : CUBLAS_OP_T;
  CUBLAS_CHECK(cublasSgemm(Caffe::cublas_handle(), cuTransB, cuTransA,
      N, M, K, &alpha, B, ldb, A, lda, &beta, C, N));
}

template <>
void caffe_gpu_gemm<double>(const CBLAS_TRANSPOSE TransA,
    const CBLAS_TRANSPOSE TransB, const int M, const int N, const int K,
    const double alpha, const double* A, const double* B, const double beta,
    double* C) {
  // Note that cublas follows fortran order.
  int lda = (TransA == CblasNoTrans) ? K : M;
  int ldb = (TransB == CblasNoTrans) ? N : K;
  cublasOperation_t cuTransA =
      (TransA == CblasNoTrans) ? CUBLAS_OP_N : CUBLAS_OP_T;
  cublasOperation_t cuTransB =
      (TransB == CblasNoTrans) ? CUBLAS_OP_N : CUBLAS_OP_T;
  CUBLAS_CHECK(cublasDgemm(Caffe::cublas_handle(), cuTransB, cuTransA,
      N, M, K, &alpha, B, ldb, A, lda, &beta, C, N));
}
#endif

template <>
void caffe_cpu_gemv<float>(const CBLAS_TRANSPOSE TransA, const int M,
    const int N, const float alpha, const float* A, const float* x,
    const float beta, float* y) {
#ifdef USE_EIGEN
	MAP_CONST_SMATRIX(eA, A, M, N);
	if(TransA == CblasNoTrans){
		MAP_SVECTOR(eY, y, M);
		eY *= beta;
		MAP_CONST_SVECTOR(eX, x, N);
		eY.noalias() += alpha * (eA * eX);
	}else{
		MAP_SVECTOR(eY, y, N);
		eY *= beta;
		MAP_CONST_SVECTOR(eX, x, M);
		eY.noalias() += alpha * (eA.transpose() * eX);
	}
#else
  cblas_sgemv(CblasRowMajor, TransA, M, N, alpha, A, N, x, 1, beta, y, 1);
#endif
}

template <>
void caffe_cpu_gemv<double>(const CBLAS_TRANSPOSE TransA, const int M,
    const int N, const double alpha, const double* A, const double* x,
    const double beta, double* y) {
#ifdef USE_EIGEN
	MAP_CONST_DMATRIX(eA, A, M, N);
	if(TransA == CblasNoTrans){
		MAP_DVECTOR(eY, y, M);
		eY *= beta;
		MAP_CONST_DVECTOR(eX, x, N);
		eY.noalias() += alpha * (eA * eX);
	}else{
		MAP_DVECTOR(eY, y, N);
		eY *= beta;
		MAP_CONST_DVECTOR(eX, x, M);
		eY.noalias() += alpha * (eA.transpose() * eX);
	}
#else
 cblas_dgemv(CblasRowMajor, TransA, M, N, alpha, A, N, x, 1, beta, y, 1);
#endif
}

#if 0
template <>
void caffe_gpu_gemv<float>(const CBLAS_TRANSPOSE TransA, const int M,
    const int N, const float alpha, const float* A, const float* x,
    const float beta, float* y) {
  cublasOperation_t cuTransA =
      (TransA == CblasNoTrans) ? CUBLAS_OP_T : CUBLAS_OP_N;
  CUBLAS_CHECK(cublasSgemv(Caffe::cublas_handle(), cuTransA, N, M, &alpha,
      A, N, x, 1, &beta, y, 1));
}

template <>
void caffe_gpu_gemv<double>(const CBLAS_TRANSPOSE TransA, const int M,
    const int N, const double alpha, const double* A, const double* x,
    const double beta, double* y) {
  cublasOperation_t cuTransA =
      (TransA == CblasNoTrans) ? CUBLAS_OP_T : CUBLAS_OP_N;
  CUBLAS_CHECK(cublasDgemv(Caffe::cublas_handle(), cuTransA, N, M, &alpha,
      A, N, x, 1, &beta, y, 1));
}
#endif

template <>
void caffe_axpy<float>(const int N, const float alpha, const float* X,
    float* Y) { 
#ifdef USE_EIGEN
	MAP_SVECTOR(eY, Y, N);
	MAP_CONST_SVECTOR(eX, X, N);
	eY = alpha * eX + eY;
#else
	cblas_saxpy(N, alpha, X, 1, Y, 1); 
#endif
}

template <>
void caffe_axpy<double>(const int N, const double alpha, const double* X,
    double* Y) 
{
#ifdef USE_EIGEN
	MAP_DVECTOR(eY, Y, N);
	MAP_CONST_DVECTOR(eX, X, N);
	eY = alpha * eX + eY;
#else
	cblas_daxpy(N, alpha, X, 1, Y, 1); 
#endif
}

template <typename Dtype>
void caffe_set(const int N, const Dtype alpha, Dtype* Y) {
  if (alpha == 0) {
    memset(Y, 0, sizeof(Dtype) * N);  // NOLINT(caffe/alt_fn)
    return;
  }
  for (int i = 0; i < N; ++i) {
    Y[i] = alpha;
  }
}

template void caffe_set<int>(const int N, const int alpha, int* Y);
template void caffe_set<float>(const int N, const float alpha, float* Y);
template void caffe_set<double>(const int N, const double alpha, double* Y);

template <>
void caffe_add_scalar(const int N, const float alpha, float* Y) {
  for (int i = 0; i < N; ++i) {
    Y[i] += alpha;
  }
}

template <>
void caffe_add_scalar(const int N, const double alpha, double* Y) {
  for (int i = 0; i < N; ++i) {
    Y[i] += alpha;
  }
}


#if 0
template <>
void caffe_gpu_axpy<float>(const int N, const float alpha, const float* X,
    float* Y) {
  CUBLAS_CHECK(cublasSaxpy(Caffe::cublas_handle(), N, &alpha, X, 1, Y, 1));
}

template <>
void caffe_gpu_axpy<double>(const int N, const double alpha, const double* X,
    double* Y) {
  CUBLAS_CHECK(cublasDaxpy(Caffe::cublas_handle(), N, &alpha, X, 1, Y, 1));
}
#endif

template <>
void caffe_cpu_axpby<float>(const int N, const float alpha, const float* X,
    const float beta, float* Y) {
#ifdef USE_EIGEN
	MAP_SVECTOR(eY, Y, N);
	MAP_CONST_SVECTOR(eX, X, N);
	eY = alpha * eX + beta * eY;
#else
  cblas_saxpby(N, alpha, X, 1, beta, Y, 1);
#endif
}

template <>
void caffe_cpu_axpby<double>(const int N, const double alpha, const double* X,
    const double beta, double* Y) {
#ifdef USE_EIGEN
	MAP_DVECTOR(eY, Y, N);
	MAP_CONST_DVECTOR(eX, X, N);
	eY = alpha * eX + beta * eY;
#else
  cblas_daxpby(N, alpha, X, 1, beta, Y, 1);
#endif
}

template <typename Dtype>
void caffe_copy(const int N, const Dtype* X, Dtype* Y) {
  if (X != Y) {
    if (Caffe::mode() == Caffe::GPU) {
#ifndef CPU_ONLY
      // NOLINT_NEXT_LINE(caffe/alt_fn)
      CUDA_CHECK(cudaMemcpy(Y, X, sizeof(Dtype) * N, cudaMemcpyDefault));
#else
      NO_GPU;
#endif
    } else {
      memcpy(Y, X, sizeof(Dtype) * N);  // NOLINT(caffe/alt_fn)
    }
  }
}

template void caffe_copy<int>(const int N, const int* X, int* Y);
template void caffe_copy<unsigned int>(const int N, const unsigned int* X,
    unsigned int* Y);
template void caffe_copy<float>(const int N, const float* X, float* Y);
template void caffe_copy<double>(const int N, const double* X, double* Y);

#if 0
template <>
void caffe_gpu_copy<float>(const int N, const float* X, float* Y) {
  CUBLAS_CHECK(cublasScopy(Caffe::cublas_handle(), N, X, 1, Y, 1));
}

template <>
void caffe_gpu_copy<double>(const int N, const double* X, double* Y) {
  CUBLAS_CHECK(cublasDcopy(Caffe::cublas_handle(), N, X, 1, Y, 1));
}
#endif

template <>
void caffe_scal<float>(const int N, const float alpha, float *X) {
#ifdef USE_EIGEN
	MAP_SVECTOR(eX, X, N);
	eX *= alpha;
#else
  cblas_sscal(N, alpha, X, 1);
#endif
}

template <>
void caffe_scal<double>(const int N, const double alpha, double *X) {
#ifdef USE_EIGEN
	MAP_DVECTOR(eX, X, N);
	eX *= alpha;
#else
  cblas_dscal(N, alpha, X, 1);
#endif
}

#if 0
template <>
void caffe_gpu_scal<float>(const int N, const float alpha, float *X) {
  CUBLAS_CHECK(cublasSscal(Caffe::cublas_handle(), N, &alpha, X, 1));
}

template <>
void caffe_gpu_scal<double>(const int N, const double alpha, double *X) {
  CUBLAS_CHECK(cublasDscal(Caffe::cublas_handle(), N, &alpha, X, 1));
}

template <>
void caffe_gpu_axpby<float>(const int N, const float alpha, const float* X,
    const float beta, float* Y) {
  caffe_gpu_scal<float>(N, beta, Y);
  caffe_gpu_axpy<float>(N, alpha, X, Y);
}

template <>
void caffe_gpu_axpby<double>(const int N, const double alpha, const double* X,
    const double beta, double* Y) {
  caffe_gpu_scal<double>(N, beta, Y);
  caffe_gpu_axpy<double>(N, alpha, X, Y);
}
#endif

template <>
void caffe_sqr<float>(const int n, const float* a, float* y) {
  vsSqr(n, a, y);
}

template <>
void caffe_sqr<double>(const int n, const double* a, double* y) {
  vdSqr(n, a, y);
}

template <>
void caffe_add<float>(const int n, const float* a, const float* b,
    float* y) { vsAdd(n, a, b, y); }

template <>
void caffe_add<double>(const int n, const double* a, const double* b,
    double* y) { vdAdd(n, a, b, y); }

template <>
void caffe_sub<float>(const int n, const float* a, const float* b,
    float* y) { vsSub(n, a, b, y); }

template <>
void caffe_sub<double>(const int n, const double* a, const double* b,
    double* y) { vdSub(n, a, b, y); }

template <>
void caffe_mul<float>(const int n, const float* a, const float* b,
    float* y) { vsMul(n, a, b, y); }

template <>
void caffe_mul<double>(const int n, const double* a, const double* b,
    double* y) { vdMul(n, a, b, y); }

template <>
void caffe_div<float>(const int n, const float* a, const float* b,
    float* y) { vsDiv(n, a, b, y); }

template <>
void caffe_div<double>(const int n, const double* a, const double* b,
    double* y) { vdDiv(n, a, b, y); }

template <>
void caffe_powx<float>(const int n, const float* a, const float b,
    float* y) { vsPowx(n, a, b, y); }

template <>
void caffe_powx<double>(const int n, const double* a, const double b,
    double* y) { vdPowx(n, a, b, y); }

#if 0
template <>
void caffe_vRngUniform<float>(const int n, float* r,
    const float a, const float b) {
  VSL_CHECK(vsRngUniform(VSL_RNG_METHOD_UNIFORM_STD, Caffe::vsl_stream(),
      n, r, a, b));
}

template <>
void caffe_vRngUniform<double>(const int n, double* r,
    const double a, const double b) {
  VSL_CHECK(vdRngUniform(VSL_RNG_METHOD_UNIFORM_STD, Caffe::vsl_stream(),
      n, r, a, b));
}

template <>
void caffe_vRngGaussian<float>(const int n, float* r, const float a,
    const float sigma) {
  VSL_CHECK(vsRngGaussian(VSL_RNG_METHOD_GAUSSIAN_BOXMULLER,
      Caffe::vsl_stream(), n, r, a, sigma));
}


template <>
void caffe_vRngGaussian<double>(const int n, double* r, const double a,
    const double sigma) {
  VSL_CHECK(vdRngGaussian(VSL_RNG_METHOD_GAUSSIAN_BOXMULLER,
      Caffe::vsl_stream(), n, r, a, sigma));
}
#endif

template <>
void caffe_exp<float>(const int n, const float* a, float* y) {
  vsExp(n, a, y);
}

template <>
void caffe_exp<double>(const int n, const double* a, double* y) {
  vdExp(n, a, y);
}

template <>
float caffe_cpu_dot<float>(const int n, const float* x, const float* y) {
#ifdef USE_EIGEN
	MAP_CONST_SVECTOR(eX, x, n);
	MAP_CONST_SVECTOR(eY, y, n);
	return eX.dot(eY);
#else
  return cblas_sdot(n, x, 1, y, 1);
#endif
}

template <>
double caffe_cpu_dot<double>(const int n, const double* x, const double* y) {
#ifdef USE_EIGEN
	MAP_CONST_DVECTOR(eX, x, n);
	MAP_CONST_DVECTOR(eY, y, n);
	return eX.dot(eY);
#else
  return cblas_ddot(n, x, 1, y, 1);
#endif
}

template <>
float caffe_cpu_asum<float>(const int n, const float* x) {
#ifdef USE_EIGEN
  MAP_CONST_SVECTOR(eX, x, n);
  return eX.cwiseAbs().sum();
#else
  return cblas_sasum(n, x, 1);
#endif
}

template <>
double caffe_cpu_asum<double>(const int n, const double* x) {
#ifdef USE_EIGEN
  MAP_CONST_DVECTOR(eX, x, n);
  return eX.cwiseAbs().sum();
#else
  return cblas_dasum(n, x, 1);
#endif
}

template <>
void caffe_cpu_scale<float>(const int n, const float alpha, const float *x,
                            float* y) {
#ifdef USE_EIGEN
  MAP_CONST_SVECTOR(eX, x, n);
  MAP_SVECTOR(eY, y, n);
  eY = eX.array() * alpha;
#else
  cblas_scopy(n, x, 1, y, 1);
  cblas_sscal(n, alpha, y, 1);
#endif
}

template <>
void caffe_cpu_scale<double>(const int n, const double alpha, const double *x,
                             double* y) {
#ifdef USE_EIGEN
  MAP_CONST_DVECTOR(eX, x, n);
  MAP_DVECTOR(eY, y, n);
  eY = eX.array() * alpha;
#else
  cblas_dcopy(n, x, 1, y, 1);
  cblas_dscal(n, alpha, y, 1);
#endif
}


#if 0 
template <>
void caffe_gpu_dot<float>(const int n, const float* x, const float* y,
    float* out) {
  CUBLAS_CHECK(cublasSdot(Caffe::cublas_handle(), n, x, 1, y, 1, out));
}

template <>
void caffe_gpu_dot<double>(const int n, const double* x, const double* y,
    double * out) {
  CUBLAS_CHECK(cublasDdot(Caffe::cublas_handle(), n, x, 1, y, 1, out));
}
#endif

}  // namespace caffe
