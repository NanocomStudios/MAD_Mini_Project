import time
import ssl
import paho.mqtt.client as mqtt

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

try:
    # 7. Publish messages periodically
    while True:
        payload = "23.5°C"
        print(f"Publishing data: {payload}")
        client.publish(TOPIC, payload=payload, qos=1)
        time.sleep(5) # Delay between publishes
        
except KeyboardInterrupt:
    print("Disconnecting...")
    # Clean shutdown
    client.loop_stop()
    client.disconnect()
