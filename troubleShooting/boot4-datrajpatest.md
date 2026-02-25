🧩 Troubleshooting: Spring Boot 4 + @DataJpaTest Classpath 이슈 해결기
📌 문제 상황

Spring Boot 4.0.2 기반 프로젝트에서 @DataJpaTest를 사용한 JPA slice 테스트를 작성한 뒤,

./gradlew test

실행 시 아래 컴파일 에러가 발생했다.

org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest not found

테스트 실행 이전, 컴파일 단계에서 실패.

1 증상의 핵심

@DataJpaTest import 자체가 해석되지 않음

즉, 테스트 런타임 문제가 아니라 classpath 누락 문제

테스트 의존성이 실제로 testCompileClasspath에 올라가지 않음

2 Spring Boot 4의 테스트 구조 변화

Boot 4에서는 기술 영역별 테스트 스타터 사용이 권장된다.

즉,

목적	권장 스타터
일반 테스트	spring-boot-starter-test
JPA slice 테스트	spring-boot-starter-data-jpa-test

기존처럼 spring-boot-test-autoconfigure를 수동 추가하는 방식은
의존성 정렬 충돌을 일으킬 가능성이 있다.


🛠 2. 해결 전략
🎯 목표

@DataJpaTest 정상 사용

JPA slice 테스트 유지

운영 DB(Postgres)와 테스트 DB(H2) 분리

의존성 중복 제거 및 구조 단순화

✅ 3. 수정 내용
🔧 build.gradle 재정리
dependencies {

    // Core
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // 운영 DB
    runtimeOnly 'org.postgresql:postgresql'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testRuntimeOnly 'com.h2database:h2'

    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

🧪 4. 검증 방법
./gradlew clean test --refresh-dependencies

clean: 기존 build 산출물 제거

refresh-dependencies: 캐시된 의존성 정리

결과: @DataJpaTest 정상 인식 + 테스트 Green

🧠 5. 배운 점 (핵심 인사이트)
1️⃣ 테스트 실패는 런타임이 아니라 컴파일 단계에서 발생할 수 있다

Classpath 문제는 테스트 실행 이전에 터진다

의존성 정렬이 가장 먼저 의심 대상

2️⃣ Spring Boot major 업그레이드는 “스타터 구조”를 바꿀 수 있다

Boot 4에서:

기술별 테스트 스타터 명확화

직접 autoconfigure 추가하는 방식은 불안정

3️⃣ 테스트 DB와 운영 DB는 역할을 분리해야 한다
환경	DB
운영	PostgreSQL
테스트	H2 (in-memory)

이렇게 분리하면:

테스트 속도 ↑

CI 안정성 ↑

Docker 의존성 ↓

4️⃣ Gradle 의존성 중복은 작은 문제가 아니다

중복/충돌 의존성은:

classpath 누락

auto-configuration 충돌

예상치 못한 Bean 생성 실패

로 이어질 수 있다.

📚 아키텍처 관점에서 의미

이번 이슈는 단순 테스트 에러가 아니라:

Spring Boot 테스트 인프라 이해

JPA slice 테스트 구조 이해

classpath resolution 과정 이해

빌드 도구(Gradle) dependency tree 이해

를 요구하는 문제였다.

이는 단순 CRUD 구현보다 프레임워크 내부 동작 이해 수준을 보여주는 사례다.

🏁 결론

Boot 4 + JPA slice 테스트 시

spring-boot-starter-data-jpa-test 사용이 가장 안정적

테스트는 H2, 운영은 Postgres로 명확히 분리

의존성은 단순하게 유지할 것