from fastapi import FastAPI, HTTPException
from src.service.order_service import get_orders as service_get_orders
from src.service.order_service import get_order as service_get_order
from src.utils import create_response

app = FastAPI()


@app.get("/")
async def root():
    return {"message": "Welcome to the Order Service API"}


@app.get("/orders")
async def get_orders():
    return create_response(service_get_orders())


@app.get("/orders/{order_id}")
async def get_order_by_id(order_id: str):
    # Fetch an order by its ID

    # return 404 if not found
    order = service_get_order(order_id)
    if order is None:
        raise HTTPException(status_code=404, detail="Order not found")

    # return 200 if found
    return order
