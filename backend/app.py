# app.py

# Required imports
import os
from flask import Flask, request, jsonify
from firebase_admin import credentials, firestore, initialize_app

# Initialize Flask app
app = Flask(__name__)

# Initialize Firestore DB
cred = credentials.Certificate('key.json')
default_app = initialize_app(cred)
db = firestore.client()
todo_ref = db.collection('todos')
pins_ref = db.collection('pins')
users_ref = db.collection('users')


# GET /nearby_pins
@app.route('/nearby_pins', methods=['GET'])
def nearby_pins():
    """Returns a list of nearby pins"""
    PIN_RADIUS = 15  # miles
    try:
        username = request.json['username']
        current_location = request.json['current_location']
        pins = get_pins()
        users = get_users()
        user = next(
            (item for item in users if item['username'] == username), None)
        print(user)
        visible_pins = [
            pin for pin in pins if pin['owner_id'] in user['friends']]
        return jsonify(visible_pins), 200
    except Exception as e:
        return f"An Error Occured: {e}"


def get_pins():
    pins = []
    for doc in pins_ref.stream():
        pin = doc.to_dict()
        for collection_ref in doc.reference.collections():
            vals = [vals.to_dict()
                    for vals in collection_ref.stream()]
            pin[collection_ref.id] = vals
        pins.append(pin)
    return pins


def get_users():
    users = []
    for doc in users_ref.stream():
        user = doc.to_dict()
        for collection_ref in doc.reference.collections():
            print(collection_ref.id)
            vals = [val.to_dict()['username']
                    for val in collection_ref.stream()]
            user[collection_ref.id] = vals
        users.append(user)
    return users


@app.route('/add', methods=['POST'])
def create():
    """
        create() : Add document to Firestore collection with request body.
        Ensure you pass a custom ID as part of json body in post request,
        e.g. json={'id': '1', 'title': 'Write a blog post'}
    """
    try:
        id = request.json['id']
        todo_ref.document(id).set(request.json)
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@app.route('/list', methods=['GET'])
def read():
    """
        read() : Fetches documents from Firestore collection as JSON.
        todo : Return document that matches query ID.
        all_todos : Return all documents.
    """
    try:
        # Check if ID was passed to URL query
        todo_id = request.args.get('id')
        if todo_id:
            todo = todo_ref.document(todo_id).get()
            return jsonify(todo.to_dict()), 200
        else:
            all_todos = [doc.to_dict() for doc in todo_ref.stream()]
            return jsonify(all_todos), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@app.route('/update', methods=['POST', 'PUT'])
def update():
    """
        update() : Update document in Firestore collection with request body.
        Ensure you pass a custom ID as part of json body in post request,
        e.g. json={'id': '1', 'title': 'Write a blog post today'}
    """
    try:
        id = request.json['id']
        todo_ref.document(id).update(request.json)
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@app.route('/delete', methods=['GET', 'DELETE'])
def delete():
    """
        delete() : Delete a document from Firestore collection.
    """
    try:
        # Check for ID in URL query
        todo_id = request.args.get('id')
        todo_ref.document(todo_id).delete()
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


port = int(os.environ.get('PORT', 8080))
if __name__ == '__main__':
    app.run(threaded=True, host='0.0.0.0', port=port)
