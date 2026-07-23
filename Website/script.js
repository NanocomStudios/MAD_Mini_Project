
// 1. Connection Configurations
// For HiveMQ Public Broker (Unsecure): Use 'ws://' and port 8000
// For HiveMQ Cloud Cluster (Secure): Use 'wss://', port 8884, and provide username/password
const brokerUrl = 'wss://24e1871158284517be3fd3a18d23a9ec.s1.eu.hivemq.cloud:8884/mqtt'; 

const options = {
    clean: true,
    connectTimeout: 4000,
    clientId: 'js_websocket_client_' + Math.random().toString(16).substr(2, 8),
    username: 'mad_web', // Uncomment if using HiveMQ Cloud
    password: 'password123', // Uncomment if using HiveMQ Cloud
};

const myTopic = 'topic1';

console.log('Connecting to HiveMQ Broker...');
const client = mqtt.connect(brokerUrl, options);

// 2. Handle Connection Event
client.on('connect', () => {
    console.log('Successfully connected to HiveMQ via WebSockets!');
    
    // Subscribe to a topic
    client.subscribe(myTopic, (err) => {
        if (!err) {
            console.log(`Subscribed to topic: ${myTopic}`);
            
            // Publish a test message once subscription is active
            client.publish(myTopic, 'Hello from the browser via WebSockets!');
        } else {
            console.error('Subscription error:', err);
        }
    });
});

// 3. Handle Incoming Messages
client.on('message', (topic, message) => {
    // message is a Buffer object, convert it to a string
    console.log(`Received message on [${topic}]: ${message.toString()}`);
});

// 4. Handle Errors & Reconnections
client.on('error', (err) => {
    console.error('Connection error: ', err);
    client.end();
});

client.on('reconnect', () => {
    console.log('Attempting to reconnect...');
});

async function loadJSON() {
  const response = await fetch("json/items.json");
  const items = await response.json();

  console.log(items)
}

loadJSON();