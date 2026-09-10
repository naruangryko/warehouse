# 왕관과 잿불 (Crown & Cinder) — 0.1.0 소스 초안

**상태: 전체 모드 컴파일 및 게임 실행 미검증. 설치용 JAR은 포함하지 않습니다.**

Minecraft Java Edition 1.20.1 / Fabric Loader 0.15.11 / Fabric API 0.92.2+1.20.1 /
Java 17 JDK / Gradle 8.7 / Fabric Loom 1.6.12를 대상으로 작성했습니다.
최신 버전 추천이 아니라 Java 17 환경에서 재현 가능한 고정 버전 조합을 선택했습니다.
Bedrock Edition, Forge, NeoForge용 모드는 아닙니다.

## 이번에 작성한 기능

| 기능 | 소스 구현 범위 | 검증 상태 |
| --- | --- | --- |
| 검 3종 | 장검, 대검, 쌍날검 / 제작법 / 내구도 / 요구 레벨 / 대검 우클릭 주변 베기 | 게임 미검증 |
| 성장 | 별도 전투 XP, 레벨 1~50, 레벨당 2포인트 | 독립 규칙 검사 통과 |
| 능력치 | 힘: 피해, 체력: 최대 체력, 민첩: 이동 속도 | 배분 규칙 검사 통과, 실제 속성 적용 미검증 |
| 고블린 | 좀비 기반 AI, 자체 임시 텍스처, 평원·숲·타이가 어두운 곳 자연 스폰, 전리품 | 게임 미검증 |
| 토벌 의뢰 | 수락 후 고블린 5킬, 1회 보상, XP·에메랄드 | 순서·중복 보상 규칙 검사 통과, 게임 미검증 |
| 저장 | UUID별 NBT, 오버월드 영속 저장, 사망 시 현 레벨 XP 10% 손실 | NBT/재접속 미검증 |
| 표시 | 한국어·영어, 명령어, 액션바, 바닐라 아이템 쿨다운 | JSON 검사 통과, 화면 미검증 |

국가·기사단·용병단·던전·마왕은 **아직 구현하지 않았습니다**. `DESIGN.md`에
세계관과 3~5단계 계획, 구현 완료 기준을 정리했습니다.
1·2단계 역시 전체 실행 검증이 끝나지 않아 완료로 표시하지 않았습니다.

## PC에서 빌드하기 (Windows)

1. 압축을 풀고 `crown-and-cinder` 폴더를 엽니다.
2. **JDK 17**과 **Gradle 8.7**을 설치하고 명령줄에서 사용할 수 있게 설정합니다.
   Gradle 공식 배포: https://downloads.gradle.org/distributions/gradle-8.7-bin.zip
   `java -version`, `javac -version`, `gradle --version`으로 확인합니다.
3. 해당 폴더에서 PowerShell을 열고 실행합니다.

```powershell
gradle --no-daemon build
gradle runClient
```

인터넷 연결이 필요합니다. 첫 실행은 Minecraft·Fabric·Gradle 의존성 다운로드로
시간이 걸립니다. Gradle Wrapper는 이번 환경에서 생성하지 못해 포함하지 않았습니다.
프로젝트가 정상 구성되는 PC에서는 `gradle wrapper --gradle-version 8.7`로 추가할 수 있습니다.
이후 `./gradlew.bat build`를 사용할 수 있습니다.

빌드가 성공하면 설치 대상은 `build/libs/crown-and-cinder-0.1.0.jar`입니다.
`-sources.jar`는 설치 파일이 아닙니다. 이 파일의 생성은 여기서 확인하지 못했습니다.

Fabric 1.20.1 게임 프로필의 `mods` 폴더에 빌드한 JAR과 해당 버전의 Fabric API를
함께 넣습니다. 멀티플레이에서는 서버와 모든 클라이언트에 동일한 버전을 설치합니다.
기존 월드 원본이 아니라 **백업 복사본**에서 먼저 시험하세요.

개발 서버 작업은 `gradle runServer`입니다. 서버를 이용하려면 본인이 Minecraft
EULA를 읽고 동의해야 합니다. 이 프로젝트는 EULA를 대신 수락하지 않습니다.

## 조작 방법 (빌드 및 로딩이 성공한 후)

기본 공격은 좌클릭이며, 대검의 우클릭은 반경 3블록 내 보이는 적대 몬스터를
공격하고 5초 재사용 대기시간과 내구도 2를 소비합니다. PvP 대상은 제외합니다.

| 명령 | 기능 |
| --- | --- |
| `/rpg` 또는 `/rpg stats` | 전투 레벨, XP, 능력치, 포인트 |
| `/rpg spend strength` | 힘에 1포인트 |
| `/rpg spend vitality` | 체력에 1포인트 |
| `/rpg spend agility` | 민첩에 1포인트 |
| `/rpg quest` | 토벌 의뢰 상태 |
| `/rpg quest accept` | 고블린 5마리 토벌 수락 |
| `/rpg quest claim` | 완료 보상 1회 수령 |
| `/rpg hud` | 액션바 표시 켜기/끄기 |

초기 요구 레벨: 장검 1, 쌍날검 2, 대검 3. 기본 제작 재료는 철괴와 막대기입니다.
정확한 배치는 `src/main/resources/data/crowncinder/recipes/`를 참조하세요.
쌍날검은 단일 아이템이며 양손 각각의 공격 애니메이션은 구현하지 않았습니다.

테스트 월드에서 관리자 권한이 있다면:

```text
/give @s crowncinder:longsword
/give @s crowncinder:greatsword
/give @s crowncinder:twinblade
/summon crowncinder:goblin ~3 ~ ~
```

전투 XP와 의뢰 킬은 **서바이벌/어드벤처 플레이어의 직접 처치**에서 지급합니다.
크리에이티브에서는 요구 레벨이 면제되지만 킬 경험치·의뢰 실적을 지급하지 않습니다.
낙사·환경 피해·자동 농장·다른 플레이어의 처치는 자신의 실적이 아닙니다.

## 주요 설정 변경

첫 모드 초기화 시 게임 폴더의 `config/crowncinder.json`을 만듭니다.
게임/서버를 종료한 뒤 수정하고 다시 시작하세요. 서버 설정이 판정 기준입니다.

| 설정 | 기본값 | 의미 |
| --- | --- | --- |
| `xpBase` | 40 | 다음 레벨 요구 XP = 값 × 현재 레벨 |
| `xpMultiplier` | 1.0 | 토벌·처치 XP 배율 |
| `goblinXp` / `otherMonsterXp` | 18 / 5 | 고블린 / 다른 적대 몬스터 기본 XP |
| `questXp` / `questEmeralds` | 100 / 3 | 토벌 보상 |
| `deathXpLoss` | 0.1 | 현재 레벨에 누적된 XP 손실 비율, 레벨 유지 |
| `damagePerStrength` | 0.25 | 힘 1포인트당 공격 피해 |
| `healthPerVitality` | 1.0 | 체력 1포인트당 최대 체력 (1 = 반 하트) |
| `speedPerAgility` | 0.003 | 민첩당 이동 속성 가산, 총 가산 상한 0.1 |
| `goblinSpawnWeight` | 12 | 스폰 상대 가중치, 0이면 자연 스폰 해제 |
| `goblinHealth` / `goblinDamage` | 16 / 3 | 고블린 기본 체력 / 공격 속성 |
| `goblinSpeed` | 0.27 | 고블린 이동 속성 |
| `greatswordLevel` / `twinbladeLevel` | 3 / 2 | 요구 레벨 |
| `cleaveDamage` / `cleaveRadius` | 4 / 3 | 대검 우클릭 피해 / 반경 |
| `cleaveCooldownTicks` | 100 | 재사용 간격 (20틱 = 1초 기준) |
| `hudIntervalTicks` | 40 | 액션바 갱신 간격, 20틱 단위로 내림 |

몹 난이도는 위 체력·공격·스폰 설정과 바닐라 난이도로 조정합니다.
잘못된 설정 파일은 덮어쓰지 않고 경고와 기본값을 사용합니다.
무기별 기본 공격 수치·속도·내구도는 현재 `item/ModItems.java` 한 곳에 모여 있으며,
제작법/전리품은 데이터팩 경로, 이름/표시는 번역 파일에서 변경할 수 있습니다.

## 테스트 및 개발

Minecraft 다운로드 없이 성장 규칙만 검사:

```powershell
java tools/TestCore.java
```

JDK compiler 모듈이 필요합니다. 기대 출력: `PASS: 15 core progression checks`.
전체 Gradle 빌드에서도 `coreTest`를 `check`에 연결했습니다.

임시 텍스처 재생성 (직접 작성한 Java 픽셀 생성기):

```powershell
java tools/GenerateTextures.java
```

이번 환경의 실제 결과와 후속 수동 테스트 목록은 `TEST_REPORT.md`를 참조하세요.
