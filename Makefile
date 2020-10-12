# Bring up docker images for enrichment app and initialise database
startinfra:
	docker-compose -f docker-compose up --build

# Compile Scala Application
test:
	gradle

# Run Main Streaming Pipeline
runapp:
	make startinfra
	gradle run

# Disabled for now, Some sort of docker networking issue on windows/DNS meant the container couldn't read from the FTP
# Server
runappdocker:
	make test
	docker build -t ais_poc
	docker run ais_poc

# Get a URL for the auto generated documentation from fast API
getenrichdocs:
	echo "http://localhost:8000/docs"

getconnectioninfo:
	make startinfra
	echo "psql -d ais_poc -U postgres -h localhost -p 5430"
	echo "password is 'goodsecurity'"



