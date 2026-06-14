# Kotlin 언어 레퍼런스 스킬

> 기준 버전: **Kotlin 2.3.21** (2026-04-23 릴리즈, 최신 안정화)
> 공식 문서: https://kotlinlang.org/docs/home.html

---

## 핵심 언어 특징

### Null Safety
```kotlin
val name: String = "Kotlin"         // Non-nullable (컴파일 타임 보장)
val nullable: String? = null        // Nullable
val length = nullable?.length       // Safe call → null 반환
val result = nullable ?: "default"  // Elvis operator
val forced = nullable!!             // Non-null assertion (NPE 가능, 지양)
```

### 타입 시스템 & 스마트 캐스트
```kotlin
fun describe(obj: Any): String = when (obj) {
    is String -> "문자열, 길이=${obj.length}"  // 스마트 캐스트
    is Int    -> "정수: $obj"
    else      -> "기타"
}
```

### Data Class
```kotlin
data class User(val id: Long, val name: String, val email: String)
// 자동 생성: equals(), hashCode(), toString(), copy(), componentN()
val user = User(1, "Heevis", "dev@example.com")
val updated = user.copy(email = "new@example.com")
val (id, name, _) = user  // 구조 분해
```

### Sealed Class / Interface
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// when은 else 없이 exhaustive 처리 가능
fun handle(result: Result<String>) = when (result) {
    is Result.Success  -> println(result.data)
    is Result.Failure  -> println(result.error.message)
    Result.Loading     -> println("로딩 중...")
}
```

### Extension Functions
```kotlin
fun String.isEmail(): Boolean = contains("@") && contains(".")
fun List<Int>.average(): Double = sum().toDouble() / size

"user@example.com".isEmail()  // true
```

### Higher-Order Functions & Lambdas
```kotlin
val doubled = listOf(1, 2, 3).map { it * 2 }
val evens   = listOf(1, 2, 3, 4).filter { it % 2 == 0 }
val sum     = listOf(1, 2, 3).fold(0) { acc, n -> acc + n }

// 함수 타입
fun apply(value: Int, transform: (Int) -> Int): Int = transform(value)
apply(5) { it * 3 }  // 15
```

### Object & Companion Object
```kotlin
object Singleton {
    fun doWork() = println("싱글턴 작업")
}

class MyClass {
    companion object {
        const val TAG = "MyClass"
        fun create() = MyClass()
    }
}
```

### Scope Functions
| 함수 | 컨텍스트 | 반환 | 주요 용도 |
|------|---------|------|---------|
| `let` | `it` | 람다 결과 | null 체크 후 변환 |
| `run` | `this` | 람다 결과 | 초기화 + 계산 |
| `with` | `this` | 람다 결과 | 객체 여러 속성 접근 |
| `apply` | `this` | 수신 객체 | 빌더 패턴 |
| `also` | `it` | 수신 객체 | 사이드 이펙트 |

```kotlin
val user = User(1, "Heevis", "").also { println("생성: $it") }
    .apply { /* email = "..." */ }
```

### 컬렉션 API
```kotlin
val list = listOf(1, 2, 3)         // 불변
val mList = mutableListOf(1, 2, 3) // 가변
val map = mapOf("a" to 1, "b" to 2)

list.map { it * 2 }
    .filter { it > 2 }
    .groupBy { if (it > 4) "big" else "small" }
    .forEach { (k, v) -> println("$k: $v") }
```

---

## 타입 & 제네릭

```kotlin
// 공변 (out) / 반공변 (in)
class Box<out T>(val value: T)
fun <T : Comparable<T>> max(a: T, b: T): T = if (a > b) a else b

// reified 타입 파라미터
inline fun <reified T> List<*>.filterIsType(): List<T> =
    filterIsInstance<T>()
```

---

## 주요 키워드 요약

| 키워드 | 설명 |
|-------|------|
| `val` / `var` | 불변 / 가변 프로퍼티 |
| `suspend` | 코루틴 일시정지 함수 |
| `inline` | 람다 인라이닝 최적화 |
| `tailrec` | 꼬리 재귀 최적화 |
| `operator` | 연산자 오버로딩 |
| `infix` | 중위 표기 함수 |
| `crossinline` | 인라인 람다 non-local return 방지 |

---

## Gradle 의존성 (최신 안정화)

```kotlin
plugins {
    kotlin("jvm") version "2.3.21"
}
```

## 참고 링크
- 공식 문서: https://kotlinlang.org/docs/
- API 레퍼런스: https://kotlinlang.org/api/latest/jvm/stdlib/
- Kotlin Playground: https://play.kotlinlang.org/
