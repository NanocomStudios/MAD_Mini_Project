import time
import ssl
import paho.mqtt.client as mqtt

from fastapi import FastAPI
from pydantic import BaseModel
import hashlib
import sqlite3

import threading

import firebase_admin
from firebase_admin import credentials, messaging

import json

from fastapi.middleware.cors import CORSMiddleware

cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred)


# message = messaging.Message(
#     notification=messaging.Notification(title="Broadcast", body="Hello users!"),
#     topic="news", # Target topic
# )
# response = messaging.send(message)

conn = sqlite3.connect('items.db')
c = conn.cursor()

c.execute("CREATE TABLE IF NOT EXISTS users (\
            userID INTEGER PRIMARY KEY AUTOINCREMENT,\
            username TEXT UNIQUE, password TEXT\
          )")

c.execute("CREATE TABLE IF NOT EXISTS sessions (\
            sessionID TEXT PRIMARY KEY,\
            userID INTEGER NOT NULL,\
            expire TEXT DEFAULT (datetime('now', '+2 days')),\
            deviceID TEXT NOT NULL,\
            FOREIGN KEY(userID) REFERENCES users(userID)\
          )")

c.execute("CREATE TABLE IF NOT EXISTS items (\
            itemID INTEGER PRIMARY KEY,\
            itemName TEXT NOT NULL,\
            type TEXT NOT NULL,\
            state TEXT NOT NULL DEFAULT '0',\
            lastOnTime TEXT NOT NULL DEFAULT '0',\
            cuttoffTime TEXT NOT NULL DEFAULT '0'\
          )")

c.execute("CREATE TABLE IF NOT EXISTS rooms (\
            roomID INTEGER PRIMARY KEY AUTOINCREMENT,\
            roomName TEXT NOT NULL,\
            userID INTEGER NOT NULL,\
            FOREIGN KEY(userID) REFERENCES users(userID)\
          )")

c.execute("CREATE TABLE IF NOT EXISTS room_items (\
            roomID INTEGER NOT NULL,\
            itemID INTEGER NOT NULL,\
            PRIMARY KEY (roomID, itemID),\
            FOREIGN KEY(roomID) REFERENCES rooms(roomID),\
            FOREIGN KEY(itemID) REFERENCES items(itemID)\
            )")



# Configuration settings
# Replace with your HiveMQ Cloud Cluster URL (e.g., "xxxxxx.s1.eu.hivemq.cloud")
BROKER_URL = "24e1871158284517be3fd3a18d23a9ec.s1.eu.hivemq.cloud" 
PORT = 8883 # Secure TLS port
USERNAME = "mad_py"
PASSWORD = "password123"

# Callback triggered when the client connects to the broker
def on_connect(client, userdata, flags, rc, properties=None):
    if rc == 0:
        print("Successfully connected to HiveMQ!")
        # Subscribe to your target topic upon connection
        client.subscribe("broadcast/server", qos=1)
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

def itemUpdateLoop():
    while True:
        payload = {"request":"update"}
        client.publish("broadcast/item" , payload=json.dumps(payload), qos=1)
        time.sleep(600) # 10 minutes refresh

itemUpdateThread = threading.Thread(target=itemUpdateLoop)
itemUpdateThread.daemon = True
itemUpdateThread.start()

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Your frontend URL
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class User(BaseModel):
    username: str
    password: str
    deviceID: str

class Session(BaseModel):
    sessionID: str

class Item(BaseModel):
    itemID: int
    itemName: str
    itemType: str

class AppAction(BaseModel):
    itemID: int
    action: str
    value: int
    sessionID: str

class AppRoom(BaseModel):
    roomName: str
    sessionID: str

class RoomItem(BaseModel):
    roomName: str
    itemID: int
    sessionID: str

class ItemAction(BaseModel):
    itemID: int
    action: str
    value: int

class Update(BaseModel):
    itemID: int
    value: int

def isSessionValid(sessionID: str):
    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:
        c.execute("DELETE FROM sessions WHERE expire < datetime('now')")
        conn.commit()
        c.execute("SELECT * FROM sessions WHERE sessionID=?", (sessionID,))
        result = c.fetchone()

        if result:
            c.execute("UPDATE sessions SET expire=(datetime('now', '+2 days')) WHERE sessionID=?", (sessionID,))
            conn.commit()

        conn.close()
        return result is not None
    except sqlite3.IntegrityError:
        print("Error: session validation failed!")
        conn.close()
        return False


    
    

@app.post("/user/login")
def login(user: User):
    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    c.execute("SELECT * FROM users WHERE username=? AND password=?", (user.username, user.password))
    result = c.fetchone()
    if result:
        sessionID = hashlib.sha256((user.username + user.password + user.deviceID + str(time.time())).encode()).hexdigest()
        c.execute("INSERT OR REPLACE INTO sessions (sessionID, userID, deviceID) VALUES (?, ?, ?)", (sessionID, result[0], user.deviceID))
        conn.commit()
        conn.close()
        return {"response": "success", "sessionID" : sessionID, "userID": result[0]}
    else:
        conn.close()
        return {"response": "failure"}

@app.post("/user/logout")
def logout(session: Session):
    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:
        c.execute("DELETE FROM sessions WHERE sessionID=?", (session.sessionID,))
        conn.commit()
        conn.close()
        return {"response": "success"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error logging out!"}

@app.post("/user/register")
def register(user: User):
    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:
        c.execute("INSERT INTO users (username, password) VALUES (?, ?)", (user.username, user.password))
        conn.commit()
        conn.close()
        return {"response": "success"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Username already exists"}

@app.post("/user/validateSession")
def validateSession(session: Session):
    return {"response": "success"} if isSessionValid(session.sessionID) else {"response": "failure"}

@app.post("/item/register")
def itemRegister(item: Item):
    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    c.execute("SELECT * FROM items WHERE itemID=?", (item.itemID,))
    result = c.fetchone()
    if result:
        if(result[2] == item.itemType):
            conn.close()
            return {"response": "success", "itemName": result[1]}
        else:
            conn.close()
            return {"response": "failure", "error": "Different item with the same item ID exists!"}
    else:
        try:
            c.execute("INSERT INTO items (itemID, itemName, type) VALUES (?, ?, ?)", (item.itemID, item.itemName, item.itemType))
            conn.commit()
            conn.close()
            return {"response": "success"}
        except sqlite3.IntegrityError:
            conn.close()
            return {"response": "failure", "error": "Error registering the item!"}

@app.post("/item/action")
def itemAction(action: ItemAction):
    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:
        c.execute("UPDATE items SET state=? WHERE itemID=?", (str(action.value), action.itemID))
        conn.commit()

        c.execute("SELECT * FROM room_items WHERE itemID=?", (action.itemID,))
        result = c.fetchall()
        for row in result:
            roomID = row[0]
            c.execute("SELECT * FROM rooms WHERE roomID=?", (roomID,))
            room = c.fetchone()
            userID = room[2]
            c.execute("SELECT * FROM sessions WHERE userID=?", (userID,))
            sessions = c.fetchall()
            for session in sessions:
                sessionID = session[0]
                message = messaging.Message(
                    notification=messaging.Notification(title="Action", body= "item/" + str(action.itemID) + " : " + action.action + " : " + str(action.value)),
                    token=session[3], # Target specific device
                )
                try:
                    response = messaging.send(message)
                    print("Successfully sent message:", response)
                except Exception as e:
                    print("Error sending message:", e)

        conn.close()

        payload = {"action":action.action, "value": action.value}
        client.publish("item/" + str(action.itemID) , payload=json.dumps(payload), qos=1)

        return {"response": "success", "action":action.action, "value": action.value} 

    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error: action on the item!"} 


@app.post("/item/update")
def itemUpdate(update: Update):
    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:
        c.execute("UPDATE items SET state=? WHERE itemID=?", (str(update.value), update.itemID))
        conn.commit()
        conn.close()
        return {"response": "success"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error updating the item!"}

@app.post("/app/action")
def appAction(action: AppAction):

    is_valid = isSessionValid(action.sessionID)

    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:

        if(is_valid):

            c.execute("UPDATE items SET state=? WHERE itemID=?", (str(action.value), action.itemID))
            conn.commit()

            c.execute("SELECT * FROM room_items WHERE itemID=?", (action.itemID,))
            result = c.fetchall()
            for row in result:
                roomID = row[0]
                c.execute("SELECT * FROM rooms WHERE roomID=?", (roomID,))
                room = c.fetchone()
                userID = room[2]
                c.execute("SELECT * FROM sessions WHERE userID=?", (userID,))
                sessions = c.fetchall()
                for session in sessions:
                    sessionID = session[0]
                    message = messaging.Message(
                        notification=messaging.Notification(title="Action", body= "item/" + str(action.itemID) + " : " + action.action + " : " + str(action.value)),
                        token=session[3], # Target specific device
                    )
                    try:
                        response = messaging.send(message)
                        print("Successfully sent message:", response)
                    except Exception as e:
                        print("Error sending message:", e)

            payload = {"action":action.action, "value": action.value}
            client.publish("item/" + str(action.itemID) , payload=json.dumps(payload), qos=1)

            conn.commit()
            conn.close()

            return {"response": "success", "action":action.action, "value": action.value}
        else:
            conn.close()
            return {"response": "failure", "error": "Invalid session ID!"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error: action on the item!"}

@app.post("/app/newRoom")
def appNewRoom(room: AppRoom):

    is_valid = isSessionValid(room.sessionID)

    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:

        if(is_valid):
            c.execute("SELECT * FROM sessions WHERE sessionID=?", (room.sessionID,))
            session = c.fetchone()
            userID = session[1]

            c.execute("INSERT INTO rooms (roomName, userID) VALUES (?, ?)", (room.roomName, userID))
            conn.commit()
            conn.close()

            return {"response": "success", "roomName": room.roomName}
        else:
            conn.close()
            return {"response": "failure", "error": "Invalid session ID!"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error: creating new room!"}

@app.post("/app/deleteRoom")
def appDeleteRoom(room: AppRoom):

    is_valid = isSessionValid(room.sessionID)

    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:

        if(is_valid):
            c.execute("SELECT * FROM sessions WHERE sessionID=?", (room.sessionID,))
            session = c.fetchone()
            userID = session[1]

            c.execute("SELECT * FROM room_items WHERE roomID=(SELECT roomID FROM rooms WHERE roomName=? AND userID=?)", (room.roomName, userID))
            items = c.fetchall()
            for item in items:
                c.execute("DELETE FROM room_items WHERE roomID=? AND itemID=?", (item[0], item[1]))

            c.execute("DELETE FROM rooms WHERE roomName=? AND userID=?", (room.roomName, userID))
            conn.commit()
            conn.close()

            return {"response": "success", "roomName": room.roomName}
        else:
            conn.close()
            return {"response": "failure", "error": "Invalid session ID!"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error: deleting the room!"}

@app.post("/app/addItemToRoom")
def appAddItemToRoom(roomItem: RoomItem):

    is_valid = isSessionValid(roomItem.sessionID)

    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:
        if(is_valid):

            c.execute("SELECT * FROM items WHERE itemID=?", (roomItem.itemID,))
            item = c.fetchone()
            if not item:
                conn.close()
                return {"response": "failure", "error": "Item not found!"}

            c.execute("SELECT * FROM room_items WHERE itemID=?", (roomItem.itemID,))
            item = c.fetchone()
            if item:
                conn.close()
                return {"response": "failure", "error": "Item already added to a room!"}

        
            c.execute("SELECT * FROM sessions WHERE sessionID=?", (roomItem.sessionID,))
            session = c.fetchone()
            userID = session[1]

            c.execute("SELECT * FROM rooms WHERE roomName=? AND userID=?", (roomItem.roomName, userID))
            room = c.fetchone()
            if room:
                roomID = room[0]
                c.execute("INSERT INTO room_items (roomID, itemID) VALUES (?, ?)", (roomID, roomItem.itemID))
                conn.commit()
                conn.close()

                return {"response": "success", "roomName": roomItem.roomName, "itemID": roomItem.itemID}
            else:
                conn.close()
                return {"response": "failure", "error": "Room not found!"}
        else:
            conn.close()
            return {"response": "failure", "error": "Invalid session ID!"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error: adding item to the room!"}

@app.post("/app/removeItemFromRoom")
def appRemoveItemFromRoom(roomItem: RoomItem):
    is_valid = isSessionValid(roomItem.sessionID)

    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:
        if(is_valid):

            c.execute("SELECT * FROM sessions WHERE sessionID=?", (roomItem.sessionID,))
            session = c.fetchone()
            userID = session[1]

            c.execute("SELECT * FROM rooms WHERE roomName=? AND userID=?", (roomItem.roomName, userID))
            room = c.fetchone()
            if room:
                roomID = room[0]
                c.execute("DELETE FROM room_items WHERE roomID=? AND itemID=?", (roomID, roomItem.itemID))
                conn.commit()
                conn.close()

                return {"response": "success", "roomName": roomItem.roomName, "itemID": roomItem.itemID}
            else:
                conn.close()
                return {"response": "failure", "error": "Room not found!"}
        else:
            conn.close()
            return {"response": "failure", "error": "Invalid session ID!"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error: removing item from the room!"}


@app.post("/app/getRooms")
def appGetRooms(session: Session):

    is_valid = isSessionValid(session.sessionID)

    conn = sqlite3.connect('items.db')
    c = conn.cursor()
    try:
        if(is_valid):

            c.execute("SELECT * FROM sessions WHERE sessionID=?", (session.sessionID,))
            session_data = c.fetchone()
            userID = session_data[1]

            c.execute("SELECT * FROM rooms WHERE userID=?", (userID,))
            rooms = c.fetchall()

            room_list = []
            for room in rooms:
                roomID = room[0]
                roomName = room[1]
                c.execute("SELECT itemID FROM room_items WHERE roomID=?", (roomID,))
                items = c.fetchall()
                itemIDs = []
                for item in items:
                    c.execute("SELECT * FROM items WHERE itemID=?", (item[0],))
                    item_data = c.fetchone()
                    if item_data:
                        itemIDs.append({"itemID": item_data[0], "itemName": item_data[1], "type": item_data[2], "state": item_data[3]})
                room_list.append({"roomName": roomName, "itemIDs": itemIDs})

            conn.close()
            return {"response": "success", "rooms": room_list}
        else:
            conn.close()
            return {"response": "failure", "error": "Invalid session ID!"}
    except sqlite3.IntegrityError:
        conn.close()
        return {"response": "failure", "error": "Error: retrieving rooms!"}

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