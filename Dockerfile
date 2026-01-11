FROM eclipse-temurin:25-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 eshop && \
    adduser -u 1001 -G eshop -s /bin/sh -D eshop

# Create necessary directories
RUN mkdir -p /var/log/eshop /var/eshop/uploads && \
    chown -R eshop:eshop /var/log/eshop /var/eshop

# Copy JAR
COPY --chown=eshop:eshop build/libs/*.jar app.jar

# Switch to non-root user
USER eshop

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Expose port
EXPOSE 8082

# JVM Options
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
