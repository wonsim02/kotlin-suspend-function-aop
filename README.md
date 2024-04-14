# `kotlin-suspend-function-aop`

AspectJ의 Load Time Weaving을 코틀린 함수에 적용한 예시입니다.
비동기(`suspend`) 함수에 대해서는 비동기 함수의 콜백을 수정하여 실제 함수 반환값이 반환될 때 혹은 예외가 발생할 때 함수 실행 종료 처리가 되도록 했습니다.

see :
- https://www.baeldung.com/aspectj
- https://github.com/JetBrains/kotlin/blob/master/compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/coroutines-codegen.md
