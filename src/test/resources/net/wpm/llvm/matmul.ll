; ModuleID = 'matmal_module'
source_filename = "matmal_module"

; Function Attrs: norecurse nounwind
define void @matmul(float* %0, float* %1, float* %2, i32 %3, i32 %4, i32 %5) {
  %7 = icmp sgt i32 %3, 0
  br i1 %7, label %8, label %85

; <label>:8:                                      ; preds = %6
  %9 = icmp sgt i32 %4, 0
  %10 = icmp sgt i32 %5, 0
  br i1 %9, label %11, label %85

; <label>:11:                                     ; preds = %8
  %12 = zext i32 %4 to i64
  %13 = shl nuw nsw i64 %12, 2
  %14 = sext i32 %4 to i64
  %15 = sext i32 %5 to i64
  %16 = zext i32 %5 to i64
  %17 = zext i32 %3 to i64
  %18 = and i64 %16, 1
  %19 = icmp eq i32 %5, 1
  %20 = sub nsw i64 %16, %18
  %21 = icmp eq i64 %18, 0
  br label %22

; <label>:22:                                     ; preds = %27, %11
  %23 = phi i64 [ %28, %27 ], [ 0, %11 ]
  %24 = mul nsw i64 %23, %14
  %25 = mul nsw i64 %23, %15
  br i1 %10, label %26, label %79

; <label>:26:                                     ; preds = %22
  br label %30

; <label>:27:                                     ; preds = %47, %79
  %28 = add nuw nsw i64 %23, 1
  %29 = icmp eq i64 %28, %17
  br i1 %29, label %85, label %22

; <label>:30:                                     ; preds = %26, %47
  %31 = phi i64 [ %51, %47 ], [ 0, %26 ]
  br i1 %19, label %33, label %32

; <label>:32:                                     ; preds = %30
  br label %53

; <label>:33:                                     ; preds = %53, %30
  %34 = phi float [ undef, %30 ], [ %75, %53 ]
  %35 = phi i64 [ 0, %30 ], [ %76, %53 ]
  %36 = phi float [ 0.000000e+00, %30 ], [ %75, %53 ]
  br i1 %21, label %47, label %37

; <label>:37:                                     ; preds = %33
  %38 = add nsw i64 %35, %25
  %39 = getelementptr inbounds float, float* %0, i64 %38
  %40 = load float, float* %39, align 4, !tbaa !2
  %41 = mul nsw i64 %35, %14
  %42 = add nsw i64 %41, %31
  %43 = getelementptr inbounds float, float* %1, i64 %42
  %44 = load float, float* %43, align 4, !tbaa !2
  %45 = fmul float %40, %44
  %46 = fadd float %36, %45
  br label %47

; <label>:47:                                     ; preds = %33, %37
  %48 = phi float [ %34, %33 ], [ %46, %37 ]
  %49 = add nsw i64 %31, %24
  %50 = getelementptr inbounds float, float* %2, i64 %49
  store float %48, float* %50, align 4, !tbaa !2
  %51 = add nuw nsw i64 %31, 1
  %52 = icmp eq i64 %51, %12
  br i1 %52, label %27, label %30

; <label>:53:                                     ; preds = %53, %32
  %54 = phi i64 [ 0, %32 ], [ %76, %53 ]
  %55 = phi float [ 0.000000e+00, %32 ], [ %75, %53 ]
  %56 = phi i64 [ %20, %32 ], [ %77, %53 ]
  %57 = add nsw i64 %54, %25
  %58 = getelementptr inbounds float, float* %0, i64 %57
  %59 = load float, float* %58, align 4, !tbaa !2
  %60 = mul nsw i64 %54, %14
  %61 = add nsw i64 %60, %31
  %62 = getelementptr inbounds float, float* %1, i64 %61
  %63 = load float, float* %62, align 4, !tbaa !2
  %64 = fmul float %59, %63
  %65 = fadd float %55, %64
  %66 = or i64 %54, 1
  %67 = add nsw i64 %66, %25
  %68 = getelementptr inbounds float, float* %0, i64 %67
  %69 = load float, float* %68, align 4, !tbaa !2
  %70 = mul nsw i64 %66, %14
  %71 = add nsw i64 %70, %31
  %72 = getelementptr inbounds float, float* %1, i64 %71
  %73 = load float, float* %72, align 4, !tbaa !2
  %74 = fmul float %69, %73
  %75 = fadd float %65, %74
  %76 = add nsw i64 %54, 2
  %77 = add i64 %56, -2
  %78 = icmp eq i64 %77, 0
  br i1 %78, label %33, label %53

; <label>:79:                                     ; preds = %22
  %80 = trunc i64 %23 to i32
  %81 = mul i32 %80, %4
  %82 = sext i32 %81 to i64
  %83 = getelementptr float, float* %2, i64 %82
  %84 = bitcast float* %83 to i8*
  call void @llvm.memset.p0i8.i64(i8* %84, i8 0, i64 %13, i32 4, i1 false)
  br label %27

; <label>:85:                                     ; preds = %27, %8, %6
  ret void
}

; Function Attrs: argmemonly nounwind
declare void @llvm.memset.p0i8.i64(i8* nocapture writeonly, i8, i64, i32, i1) #1

attributes #0 = { norecurse nounwind "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="false" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+sse3,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { argmemonly nounwind }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{!"ecc version 2017-08-23 (http://ellcc.org) based on clang version 6.0.0 (trunk 311547)"}
!2 = !{!3, !3, i64 0}
!3 = !{!"float", !4, i64 0}
!4 = !{!"omnipotent char", !5, i64 0}
!5 = !{!"Simple C++ TBAA"}