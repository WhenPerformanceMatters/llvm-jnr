; ModuleID = 'matmul20_module'
source_filename = "matmul20_module"

; Function Attrs: noinline nounwind optnone
define void @matmul(float* %0, float* %1, float* %2) {
  %4 = alloca float*, align 8
  %5 = alloca float*, align 8
  %6 = alloca float*, align 8
  %7 = alloca i32, align 4
  %8 = alloca i32, align 4
  %9 = alloca float, align 4
  %10 = alloca i32, align 4
  store float* %0, float** %4, align 8
  store float* %1, float** %5, align 8
  store float* %2, float** %6, align 8
  store i32 0, i32* %7, align 4
  br label %11

; <label>:11:                                     ; preds = %58, %3
  %12 = load i32, i32* %7, align 4
  %13 = icmp slt i32 %12, 20
  br i1 %13, label %14, label %61

; <label>:14:                                     ; preds = %11
  store i32 0, i32* %8, align 4
  br label %15

; <label>:15:                                     ; preds = %54, %14
  %16 = load i32, i32* %8, align 4
  %17 = icmp slt i32 %16, 20
  br i1 %17, label %18, label %57

; <label>:18:                                     ; preds = %15
  store float 0.000000e+00, float* %9, align 4
  store i32 0, i32* %10, align 4
  br label %19

; <label>:19:                                     ; preds = %42, %18
  %20 = load i32, i32* %10, align 4
  %21 = icmp slt i32 %20, 20
  br i1 %21, label %22, label %45

; <label>:22:                                     ; preds = %19
  %23 = load float*, float** %4, align 8
  %24 = load i32, i32* %7, align 4
  %25 = mul nsw i32 %24, 20
  %26 = load i32, i32* %10, align 4
  %27 = add nsw i32 %25, %26
  %28 = sext i32 %27 to i64
  %29 = getelementptr inbounds float, float* %23, i64 %28
  %30 = load float, float* %29, align 4
  %31 = load float*, float** %5, align 8
  %32 = load i32, i32* %10, align 4
  %33 = mul nsw i32 %32, 20
  %34 = load i32, i32* %8, align 4
  %35 = add nsw i32 %33, %34
  %36 = sext i32 %35 to i64
  %37 = getelementptr inbounds float, float* %31, i64 %36
  %38 = load float, float* %37, align 4
  %39 = fmul float %30, %38
  %40 = load float, float* %9, align 4
  %41 = fadd float %40, %39
  store float %41, float* %9, align 4
  br label %42

; <label>:42:                                     ; preds = %22
  %43 = load i32, i32* %10, align 4
  %44 = add nsw i32 %43, 1
  store i32 %44, i32* %10, align 4
  br label %19

; <label>:45:                                     ; preds = %19
  %46 = load float, float* %9, align 4
  %47 = load float*, float** %6, align 8
  %48 = load i32, i32* %7, align 4
  %49 = mul nsw i32 %48, 20
  %50 = load i32, i32* %8, align 4
  %51 = add nsw i32 %49, %50
  %52 = sext i32 %51 to i64
  %53 = getelementptr inbounds float, float* %47, i64 %52
  store float %46, float* %53, align 4
  br label %54

; <label>:54:                                     ; preds = %45
  %55 = load i32, i32* %8, align 4
  %56 = add nsw i32 %55, 1
  store i32 %56, i32* %8, align 4
  br label %15

; <label>:57:                                     ; preds = %15
  br label %58

; <label>:58:                                     ; preds = %57
  %59 = load i32, i32* %7, align 4
  %60 = add nsw i32 %59, 1
  store i32 %60, i32* %7, align 4
  br label %11

; <label>:61:                                     ; preds = %11
  ret void
}

attributes #0 = { noinline nounwind optnone "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+sse3,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{!"ecc version 2017-08-23 (http://ellcc.org) based on clang version 6.0.0 (trunk 311547)"}


