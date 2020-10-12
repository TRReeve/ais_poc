FROM python:3.8.6-slim-buster

RUN python3 -m pip install uvicorn fastapi reverse_geocoder

COPY ./app.py /app.py

EXPOSE 8000

CMD ["python3", "/app.py"]