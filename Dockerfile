# 第一階段：用 Maven + JDK 21 把專案編譯打包成一個可執行的 jar
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 先只複製 pom.xml 和 mvnw 相關檔案，跑一次 dependency 下載，
# 這樣之後只改 Java 原始碼時，Docker 可以直接複用這層 cache，不用每次都重抓全部依賴套件
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# 再複製真正的原始碼進來編譯
COPY src src
RUN ./mvnw clean package -DskipTests -B

# 第二階段：只留執行 jar 需要的 JRE（不含編譯工具），把 image 縮小
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render 會用環境變數 PORT 告訴容器要監聽哪個埠，這裡不用寫死，Spring Boot 那邊已經改成讀 ${PORT:8080}
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
