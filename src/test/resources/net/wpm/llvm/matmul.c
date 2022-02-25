void matmul(const float *a, const float *b, float *c, const int M, const int N, const int K)
{
	for (int m = 0; m < M; m++) {
  		for (int n = 0; n < N; n++) {
  			float s = 0;
  			for (int k = 0; k < K; k++) {
  				s += a[m * K + k] * b[k * N + n];
  			}
  			c[m * N + n] = s;
  		}
	}
}