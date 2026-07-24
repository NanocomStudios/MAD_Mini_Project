import time
import ssl
import paho.mqtt.client as mqtt

from fastapi import FastAPI
from pydantic import BaseModel
import hashlib
import sqlite3

from fastapi.middleware.cors import CORSMiddleware

conn = sqlite3.connect('users.db')
c = conn.cursor()

c.execute("CREATE TABLE IF NOT EXISTS users (userid INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)")
c.execute("CREATE TABLE IF NOT EXISTS sessions (session_id TEXT PRIMARY KEY, userid INTEGER, FOREIGN KEY(userid) REFERENCES users(userid))")

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8000"], # Your frontend URL
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class User(BaseModel):
    username: str
    password: str

class Session(BaseModel):
    userid: int
    sessionID: str

@app.post("/login")
def login(user: User):
    conn = sqlite3.connect('users.db')
    c = conn.cursor()
    c.execute("SELECT * FROM users WHERE username=? AND password=?", (user.username, user.password))
    result = c.fetchone()
    if result:
        session_id = hashlib.sha256((user.username + user.password + str(time.time())).encode()).hexdigest()
        c.execute("INSERT OR REPLACE INTO sessions (session_id, userid) VALUES (?, ?)", (session_id, result[0]))
        conn.commit()
        conn.close()
        return {"response": "success", "sessionID" : session_id, "userid": result[0]}
    else:
        conn.close()
        return {"response": "failure"}

@app.post("/register")
def register(user: User):
    conn = sqlite3.connect('users.db')
    c = conn.cursor()
    try:
        c.execute("INSERT INTO users (username, password) VALUES (?, ?)", (user.username, user.password))
        conn.commit()
        conn.close()
        return {"response": "success"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Username already exists"}

# Configuration settings
# Replace with your HiveMQ Cloud Cluster URL (e.g., "xxxxxx.s1.eu.hivemq.cloud")
BROKER_URL = "24e1871158284517be3fd3a18d23a9ec.s1.eu.hivemq.cloud" 
PORT = 8883 # Secure TLS port
USERNAME = "mad_py"
PASSWORD = "password123"
TOPIC = "topic1"

# Callback triggered when the client connects to the broker
def on_connect(client, userdata, flags, rc, properties=None):
    if rc == 0:
        print("Successfully connected to HiveMQ!")
        # Subscribe to your target topic upon connection
        client.subscribe(TOPIC, qos=1)
    else:
        print(f"Connection failed with code {rc}")

# Callback triggered when a message is received from a subscribed topic
def on_message(client, userdata, msg):
    print(f"Received message on '{msg.topic}': {msg.payload.decode('utf-8')}")

# Callback triggered when a message is successfully published
def on_publish(client, userdata, mid, reason_code=None, properties=None):
    print(f"Message {mid} sent successfully.")

# 1. Initialize the client using MQTTv5 (or CallbackAPIVersion.VERSION2 for Paho 2.0)
client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION2)

# 2. Assign the callback functions
client.on_connect = on_connect
client.on_message = on_message
client.on_publish = on_publish

# 3. Configure TLS Security (Required for HiveMQ Cloud)
client.tls_set(tls_version=ssl.PROTOCOL_TLS_CLIENT)

# 4. Set Authentication Credentials
client.username_pw_set(USERNAME, PASSWORD)

# 5. Connect to HiveMQ
print("Connecting to HiveMQ Broker...")
client.connect(BROKER_URL, PORT, keepalive=60)

# 6. Start the network loop in a background thread to handle incoming/outgoing messages
client.loop_start()

# try:
    # 7. Publish messages periodically
    # while True:
    #     payload = "23.5°C"
    #     print(f"Publishing data: {payload}")
    #     client.publish(TOPIC, payload=payload, qos=1)
    #     time.sleep(5) # Delay between publishes
        
# except KeyboardInterrupt:
#     print("Disconnecting...")
#     # Clean shutdown
#     client.loop_stop()
#     client.disconnect()
