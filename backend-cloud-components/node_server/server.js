const express = require('express');
const app = express();
const mongoose = require("mongoose");
const port = 3000;
const { v4: uuidv4 } = require('uuid');
const crypto = require('crypto');
const spawn = require("child_process").spawn;
require('dotenv').config();
const uri = process.env.ATLAS_URI;
const axios = require('axios');

app.use(express.json());

app.listen(port, () => {
  console.log(`Example app listening on port ${port}`);
});

mongoose.connect(uri, {useNewUrlParser: true});
mongoose.connection.once("open", () => {
    console.log("Database connected");
});

var userModel = new mongoose.model('users', new mongoose.Schema({
    user_id: String,
    name: String,
    password: String,
    sessions: Object
}));

var sessionModel = new mongoose.model('sessions', new mongoose.Schema({
    name: String,
    session_id: String,
    user_id: String,
    start_time: String,
    duration: Number,
    total_sitting: Number,
    total_good_posture: Number,
    complete: Boolean
}));

var imageModel = new mongoose.model('images', new mongoose.Schema({
    filename: String,
    content: String,
    user_id: String,
    session_id: String,
    image_id: String,
    test: Boolean,
    date: Number,
    goodPosture: Boolean,
    isSitting: Boolean,
    comments: String
}));

// Verifying whether a password is correct, comparing to the encrypted hash in the database
function verifyPassword(password, encryptedHash) {
    var salt = encryptedHash.slice(0, 16);
	var saltedPassword = password + salt;
	var hash = crypto.createHash('sha256');
	hash.update(saltedPassword)
	var temp = hash.digest('hex');

	return (Buffer.from(temp, 'hex').toString('base64') === encryptedHash.slice(16))
}

// Encrypting a password before being stored into the database
function saltHashPassword(password) {
    var salt = crypto.randomBytes(8).toString('hex');
    var hash = crypto.createHash('sha256');
    hash.update(password + salt);
    var temp = hash.digest('hex');
    var encryptedPassword = salt + Buffer.from(temp, 'hex').toString('base64');

    return encryptedPassword;
}

// Test endpoint
app.get('/', (req, res) => {
    res.send('Test');
});

// Creating a user
app.post('/user/create', async (req, res) => {
    let new_user = {};
    new_user.name = req.body.name;
    let existingUsers = await userModel.find({"name": new_user.name});
    if(existingUsers.length !== 0){
        res.status(400).send("User already exists with user");
    } else{
        new_user.password = saltHashPassword(req.body.password);
        new_user.user_id = uuidv4().slice(0,8); 
        console.log(new_user);

        await userModel(new_user).save();   
        res.json(new_user);
    }
});

// Logging in a user
app.post('/user', async (req, res) => {
    let user = {}
    user.name = req.body.name;
    user.password = req.body.password;

    let users = await userModel.find({"name": user.name});
    if(users.length == 0){
        res.status(400).send("No user with this name");
    } else{
        if(verifyPassword(user.password, users[0].password)){
            res.send(users[0]);
        } else{
            res.status(400).send("bad login");
        }
    }
});

// Getting all sessions given a user id
app.get('/sessions/:user_id', async (req, res) => {
    let user_id = req.params.user_id;
    res.json(await sessionModel.find({"user_id": user_id}));
});

// Find a specific user id and session id
app.get('/sessions/:user_id/:session_id', async (req, res) => {
    let user_id = req.params.user_id;
    let session_id = req.params.session_id;
    let result = await sessionModel.find({"user_id": user_id, "session_id": session_id});
    if(result.length == 0){
        res.status(200).send("No session")
    } else{
        res.json(result[0]);
    }
});

// Create session for a given user
app.post('/sessions/:user_id', async (req, res) => {
    let new_session = {};
    new_session.name = req.body.name;
    new_session.start_time = req.body.start_time;
    new_session.session_id = uuidv4().slice(0,8); 
    new_session.user_id = req.params.user_id; 
    new_session.complete = false;
    await sessionModel(new_session).save();   
    res.json(new_session);
});

// Delete a session for a given user
app.delete('/sessions/:user_id/:session_id', async (req, res) => {
    let user_id = req.params.user_id;
    let session_id = req.params.session_id;

    let result = await sessionModel.findOneAndDelete({"user_id":user_id, "session_id": session_id});
    if(result == null){
        res.status(400).send("Unable to delete");
    } else {
        res.status(200).send("Deleted");
    }
});

// Change the status of a session to complete, compile stats and send back
app.post('/sessions/:user_id/:session_id/status', async (req, res) => {
    let user_id = req.params.user_id;
    let session_id = req.params.session_id;
    let start_flag = req.body.start;

    let allImages = await imageModel.find({"user_id":user_id, "session_id":session_id, "test": false});
    
    let result = {"complete": true, "duration":req.body.duration};
    
    let totalImages = allImages.length;
    let totalGoodPosture = 0;
    let totalSitting = 0;
    
    for(var i = 0; i < allImages.length; i++){
        var image = allImages[i];
        if(image.isSitting){
            totalSitting++;
        }
        if(image.goodPosture){
            totalGoodPosture++;
        }
    }
    
    if(totalImages == 0){
        result.total_sitting = 0;
        result.total_good_posture = 0;
        
    } else {
        result.total_sitting = totalSitting / totalImages;
        result.total_good_posture =  totalGoodPosture / totalImages;
    }
    
    await sessionModel.findOneAndUpdate({"user_id":user_id, "session_id":session_id}, result);
    res.json(await sessionModel.findOne({"user_id":user_id, "session_id":session_id}));
});

// Adding an image for a session for a given user
app.post('/sessions/:user_id/:session_id/image', async (req, res) => {
    if(req.body.test){
        let testImage = {};
        testImage.content = req.body.content;
        testImage.filename = req.body.filename;
        testImage.user_id = req.params.user_id;
        testImage.session_id = req.params.session_id;
        testImage.image_id = uuidv4().slice(0,8);
        testImage.date = Date.now();
        testImage.test = true;
        console.log(testImage);
        await imageModel(testImage).save();
        res.json(testImage);
        return;
    }

    axios.post('http://3.137.203.87:3000/model', {"content": req.body.content})
        .then(function(response) {
            console.log(response.data);
            var temp = response.data;
            let storeResult = temp;
            storeResult.filename = req.body.filename;
            storeResult.user_id = req.params.user_id;
            storeResult.session_id = req.params.session_id;
            storeResult.image_id = uuidv4().slice(0,8);
            storeResult.test = false;
            storeResult.date = Date.now();
            imageModel(storeResult).save();
            res.json(storeResult);
        })
});

// Getting all images of a session given a user id
app.get('/sessions/:user_id/:session_id/image/all', async (req, res) => {
    let user_id = req.params.user_id;
    let session_id = req.params.session_id;

    let result = await imageModel.find({"user_id":user_id, "session_id":session_id});
    res.json(result);
});

// Getting the latest image of a session
app.get('/sessions/:user_id/:session_id/image/latest', async (req, res) => {
    let user_id = req.params.user_id;
    let session_id = req.params.session_id;
    let result = await imageModel.find({"user_id":user_id, "session_id":session_id}).sort({date:-1}).limit(1);
    res.json(result[0]);
});

// Test endpoint for processing image through ML model.
app.post('/sessions/:user_id/:session_id/image/process/test', async (req, res) => {
    let scriptResult = "";
    const python = spawn('python', ['script.py']);
    python.stdout.on('data', (data) => {
        scriptResult += data.toString();
        console.log(scriptResult);
    });

    python.on('close', async (code) => {
        await imageModel(JSON.parse(scriptResult)).save();
        res.send(scriptResult);
    });
});