const ROOT = "http://localhost:8000"

var itemList = {};

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


console.log('Connecting to HiveMQ Broker...');
const client = mqtt.connect(brokerUrl, options);

// 2. Handle Connection Event
client.on('connect', () => {
    console.log('Successfully connected to HiveMQ via WebSockets!');
    
    // Subscribe to a topic
    client.subscribe("item/broadcast", (err) => {
        if (err) {
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

async function registerItemOnServer(itemID, itemName, type){
    const payload = {
        "itemID":itemID,
        "itemName":itemName,
        "itemType":type
    }

    const url = "http://localhost:8000/item/register";
    try{
        const response = await fetch(url, {
            method: "POST",
            headers: {
            "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        if(data.response == "success"){
            return data;
        }else{
            console.log("Error");
            return null;
        }
    }catch (error){
        console.log("Error (catch)");
        return null;
    }
    


}

async function loadItems(){
    var rooms_container = document.getElementById("rooms_container");
    rooms_container.innerHTML = "";
    
    for(roomItems of items){
        var room_div = document.createElement("div");

        var room_name_section = document.createElement("div");
        var room_name = document.createElement("h2");
        room_name.innerText = roomItems.room;
        room_name_section.appendChild(room_name);

        var room_item_section = document.createElement("div");
        room_item_section.style = "display:flex;";
        
        for(const item of Object.values(roomItems.items)){
            const itemID = item.id;

            const response = await registerItemOnServer(item.id, item.name, item.type);
            if(response){
                if(response.itemName){
                    item.name = response.itemName;
                }
            }

            client.subscribe("item/" + itemID, (err) => {
                if (err) {
                    console.error('Subscription error:', err);
                }
            });

            itemList[itemID] = {
                "name":item.name,
                "type":item.type,
                "state":0
            }

            var item_card = document.createElement("div");
            var item_name = document.createElement("h3");
            item_name.innerText = item.name;

            var item_icon = document.createElement("img");

            var item_id = document.createElement("h4");
            item_id.innerText = item.id
            
            var item_action = document.createElement("button");
            item_action.innerText = item.type;

            
            item_card.appendChild(item_name);
            item_card.appendChild(item_icon);
            item_card.appendChild(item_id);
            item_card.appendChild(item_action);

            room_item_section.appendChild(item_card);
        }

        room_div.appendChild(room_name_section);
        room_div.appendChild(room_item_section);
        rooms_container.appendChild(room_div);
        
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadItems();
});