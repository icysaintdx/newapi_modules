# Build stage
FROM golang:1.21-alpine AS builder
WORKDIR /build
COPY go.mod go.sum main.go ./
RUN go mod download
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o channel-status .

# Runtime stage
FROM alpine:latest
WORKDIR /app
COPY --from=builder /build/channel-status .
EXPOSE 8080
ENV DATABASE_URL=postgresql://root:123456@postgres:5432/new-api?sslmode=disable
ENV PORT=8080
CMD ["./channel-status"]
