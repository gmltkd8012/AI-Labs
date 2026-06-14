# Kotlin Coroutines 레퍼런스 스킬

> 기준 버전: **kotlinx.coroutines 1.11.0** (2024-05-08 릴리즈, 최신 안정화)
> 공식 문서: https://kotlinlang.org/docs/coroutines-guide.html

---

## Gradle 의존성

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.11.0")   // Java 8 지원
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
```

---

## 기본 개념

### CoroutineScope & 빌더
```kotlin
// launch: 결과 없이 병렬 실행 (Job 반환)
val job = CoroutineScope(Dispatchers.Default).launch {
    delay(1000)
    println("완료")
}

// async: 결과 있는 병렬 실행 (Deferred<T> 반환)
val deferred = CoroutineScope(Dispatchers.IO).async {
    fetchData()  // suspend fun
}
val result = deferred.await()

// runBlocking: 블로킹 컨텍스트에서 코루틴 실행 (테스트/main용)
runBlocking {
    launch { delay(1000); println("done") }
}
```

### Dispatchers
| Dispatcher | 용도 |
|-----------|------|
| `Dispatchers.Main` | UI 스레드 (Android/Swing) |
| `Dispatchers.IO` | 파일/네트워크 I/O (최대 64 스레드) |
| `Dispatchers.Default` | CPU 집약적 작업 (코어 수만큼 스레드) |
| `Dispatchers.Unconfined` | 호출 스레드에서 시작, 재개 시 변경 가능 |

```kotlin
withContext(Dispatchers.IO) {
    // I/O 작업
}
```

---

## 구조적 동시성 (Structured Concurrency)

```kotlin
// coroutineScope: 모든 자식이 완료될 때까지 대기
suspend fun loadAll() = coroutineScope {
    val users  = async { fetchUsers() }
    val config = async { fetchConfig() }
    Pair(users.await(), config.await())  // 병렬 실행
}

// supervisorScope: 자식 하나 실패해도 나머지 계속 실행
suspend fun loadWithFallback() = supervisorScope {
    val primary  = async { fetchPrimary() }
    val fallback = async { fetchFallback() }
    try { primary.await() } catch (e: Exception) { fallback.await() }
}
```

---

## 취소 & 타임아웃

```kotlin
val job = launch {
    repeat(100) {
        ensureActive()          // 취소 체크 포인트
        doWork(it)
    }
}
job.cancel()                    // 취소 요청
job.join()                      // 취소 완료 대기
job.cancelAndJoin()             // 취소 + 대기 한번에

// 타임아웃
val result = withTimeout(3000L) { fetchData() }       // 초과 시 TimeoutCancellationException
val safe   = withTimeoutOrNull(3000L) { fetchData() } // 초과 시 null
```

---

## Flow (비동기 스트림)

```kotlin
// Flow 생성
fun numberFlow(): Flow<Int> = flow {
    for (i in 1..5) {
        delay(100)
        emit(i)
    }
}

// 수집
numberFlow()
    .map { it * 2 }
    .filter { it > 4 }
    .collect { println(it) }

// StateFlow / SharedFlow
val stateFlow  = MutableStateFlow(0)       // 최신 값 항상 유지
val sharedFlow = MutableSharedFlow<Int>()  // 이벤트 브로드캐스트

stateFlow.value = 42
sharedFlow.emit(1)

// flowOn: 업스트림 Dispatcher 변경
numberFlow()
    .map { heavyCompute(it) }
    .flowOn(Dispatchers.Default)
    .collect { updateUI(it) }
```

### Flow 주요 연산자
| 연산자 | 설명 |
|-------|------|
| `map` / `filter` | 변환 / 필터 |
| `onEach` | 사이드 이펙트 |
| `take(n)` | 처음 n개만 |
| `debounce(ms)` | 디바운스 |
| `distinctUntilChanged` | 중복 제거 |
| `flatMapLatest` | 최신 Flow로 전환 |
| `combine` | 여러 Flow 결합 |
| `zip` | 순서대로 쌍 결합 |
| `buffer` | 생산/소비 분리 |
| `conflate` | 느린 소비자에서 최신만 유지 |

---

## Channel

```kotlin
val channel = Channel<Int>(capacity = 10)

launch { (1..5).forEach { channel.send(it) }; channel.close() }
launch { for (v in channel) println(v) }

// produce: Fan-Out 패턴
val producer = produce {
    (1..5).forEach { send(it) }
}
```

---

## 예외 처리

```kotlin
// CoroutineExceptionHandler: launch에서 발생한 예외 처리
val handler = CoroutineExceptionHandler { _, throwable ->
    println("에러: ${throwable.message}")
}
val scope = CoroutineScope(Dispatchers.Default + handler)
scope.launch { throw RuntimeException("실패") }

// async 예외는 await() 시점에 전파
val deferred = async { riskyOperation() }
try { deferred.await() } catch (e: Exception) { println("처리됨") }

// CancellationException은 정상 취소이므로 재throw
try {
    someSuspendFun()
} catch (e: CancellationException) {
    throw e  // 반드시 재전파
} catch (e: Exception) {
    handleError(e)
}
```

---

## 테스트

```kotlin
@Test
fun `테스트 예시`() = runTest {
    val result = myRepository.fetchData()  // suspend 함수 직접 호출
    assertEquals("expected", result)

    advanceTimeBy(1000)     // 가상 시간 이동
    advanceUntilIdle()      // 모든 코루틴 완료 대기
}
```

---

## 참고 링크
- 공식 가이드: https://kotlinlang.org/docs/coroutines-guide.html
- API 레퍼런스: https://kotlinlang.org/api/kotlinx.coroutines/
- GitHub: https://github.com/Kotlin/kotlinx.coroutines
