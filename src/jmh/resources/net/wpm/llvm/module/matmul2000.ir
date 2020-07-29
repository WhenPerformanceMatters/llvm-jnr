; ModuleID = 'matmul2000_module'
source_filename = "matmul2000_module"

; Function Attrs: noinline nounwind optnone
define void @matmul(float* %0, float* %1, float* %2) {
  br label %5

; <label>:4:                                      ; preds = %8
  ret void

; <label>:5:                                      ; preds = %8, %3
  %6 = phi i64 [ 0, %3 ], [ %9, %8 ]
  %7 = mul nuw nsw i64 %6, 2000
  br label %11

; <label>:8:                                      ; preds = %13
  %9 = add nuw nsw i64 %6, 1
  %10 = icmp eq i64 %9, 2000
  br i1 %10, label %4, label %5

; <label>:11:                                     ; preds = %13, %5
  %12 = phi i64 [ 0, %5 ], [ %16, %13 ]
  br label %18

; <label>:13:                                     ; preds = %18
  %14 = add nuw nsw i64 %12, %7
  %15 = getelementptr inbounds float, float* %2, i64 %14
  store float %39, float* %15, align 4, !tbaa !2
  %16 = add nuw nsw i64 %12, 1
  %17 = icmp eq i64 %16, 2000
  br i1 %17, label %8, label %11

; <label>:18:                                     ; preds = %18, %11
  %19 = phi i64 [ 0, %11 ], [ %40, %18 ]
  %20 = phi float [ 0.000000e+00, %11 ], [ %39, %18 ]
  %21 = add nuw nsw i64 %19, %7
  %22 = getelementptr inbounds float, float* %0, i64 %21
  %23 = load float, float* %22, align 4, !tbaa !2
  %24 = mul nuw nsw i64 %19, 2000
  %25 = add nuw nsw i64 %24, %12
  %26 = getelementptr inbounds float, float* %1, i64 %25
  %27 = load float, float* %26, align 4, !tbaa !2
  %28 = fmul float %23, %27
  %29 = fadd float %20, %28
  %30 = or i64 %19, 1
  %31 = add nuw nsw i64 %30, %7
  %32 = getelementptr inbounds float, float* %0, i64 %31
  %33 = load float, float* %32, align 4, !tbaa !2
  %34 = mul nuw nsw i64 %30, 2000
  %35 = add nuw nsw i64 %34, %12
  %36 = getelementptr inbounds float, float* %1, i64 %35
  %37 = load float, float* %36, align 4, !tbaa !2
  %38 = fmul float %33, %37
  %39 = fadd float %29, %38
  %40 = add nsw i64 %19, 2
  %41 = icmp eq i64 %40, 2000
  br i1 %41, label %13, label %18
}

attributes #0 = { norecurse nounwind "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="false" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+sse3,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{!"ecc version 2017-08-23 (http://ellcc.org) based on clang version 6.0.0 (trunk 311547)"}
!2 = !{!3, !3, i64 0}
!3 = !{!"float", !4, i64 0}
!4 = !{!"omnipotent char", !5, i64 0}
!5 = !{!"Simple C/C++ TBAA"}
