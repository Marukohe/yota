clean:
	./gradlew clean

run:
	@./run.sh $(INSTANCE)

instance:
	@echo "create $(INSTANCE) monkey instances"
	@./generate_instance.sh $(INSTANCE)
