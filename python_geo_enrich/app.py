"""
Webservice to enrich ais data with location information
"""
import reverse_geocoder as rg
from typing import List
from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn

app = FastAPI()


class GeoRequest(BaseModel):
    latitude: float
    longitude: float
    mmsi: int
    timestamp: int


class GeoRequestList(BaseModel):
    geodata: List[GeoRequest]


class ResponseModel(BaseModel):
    name: str
    country_code: str
    admin_one: str
    admin_two: str
    latitude: float
    longitude: float
    timestamp: int
    mmsi: int


class ResponseCollection(BaseModel):
    response: List[ResponseModel]


def geo_lookup(lat_longs: list):
    lookups = rg.search(lat_longs)
    return lookups


def create_return_object(zip_tuple):
    enrichment = zip_tuple[0]
    original_data = zip_tuple[1]
    return dict(name=enrichment["name"],
                country_code=enrichment["cc"],
                admin_one=enrichment["admin1"],
                admin_two=enrichment["admin2"],
                latitude=original_data.latitude,
                longitude=original_data.longitude,
                timestamp=original_data.timestamp,
                mmsi=original_data.mmsi)


@app.post("/geoenrich", response_model=ResponseCollection)
def enrichment_endpoint(requests: GeoRequestList):
    # Map Request objects to tuples to go to geo_lookup
    to_tuples = list(map(lambda x: (x.latitude, x.longitude), requests.geodata))
    response = geo_lookup(to_tuples)
    zip_country_coords = zip(response, requests.geodata)
    map_to_responses = [create_return_object(x) for x in zip_country_coords]

    return dict(response=map_to_responses)


if __name__ == '__main__':
    uvicorn.run(app="app:app", host="0.0.0.0")
