from flask import Flask, request, jsonify
from utils.scanner import scan_network #component made
from utils.device_alerts import detect_new_devices #component made
import sqlite3
from flask import Flask, send_file
import seaborn as sns
import matplotlib.pyplot as plt
import io
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Initialize SQLite database
DATABASE = "database.db"

def init_db():
    conn = sqlite3.connect(DATABASE)
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS devices (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ip TEXT,
            mac TEXT,
            tag TEXT
        )
    """)
    conn.commit()
    conn.close()

init_db()

@app.route('/scan', methods=['GET'])
def scan():
    scanned_devices = scan_network()
    return jsonify(scanned_devices)

@app.route('/save', methods=['POST'])
def save_device():
    data = request.json
    conn = sqlite3.connect(DATABASE)
    cursor = conn.cursor()
    cursor.execute("INSERT INTO devices (ip, mac, tag) VALUES (?, ?, ?)",
                   (data['ip'], data['mac'], data.get('tag', '')))
    conn.commit()
    conn.close()
    return jsonify({"message": "Device saved successfully!"})

@app.route('/alerts', methods=['GET'])
def alerts():
    scanned_devices = scan_network()
    conn = sqlite3.connect(DATABASE)
    cursor = conn.cursor()
    cursor.execute("SELECT ip, mac, tag FROM devices")
    known_devices = [{"ip": row[0], "mac": row[1], "tag": row[2]} for row in cursor.fetchall()]
    conn.close()

    new_devices = detect_new_devices(scanned_devices, known_devices)
    return jsonify(new_devices)

@app.route('/generate-graph', methods=['GET'])
def generate_graph():
    try:
        # Load the example dataset
        planets = sns.load_dataset("planets")

        # Set the dark theme with teal/neon color palette
        sns.set_theme(style="darkgrid", palette="dark")  # Dark grid background
        cmap = sns.cubehelix_palette(start=2, rot=-.2, as_cmap=True)  # Neon-ish palette

        # Create the plot with customization
        g = sns.relplot(
            data=planets,
            x="distance", y="orbital_period",  # Replace with your data if needed
            hue="year", size="mass",  # Customize as needed
            palette=cmap, sizes=(10, 200),
        )

        # Apply log scale to both axes
        g.set(xscale="log", yscale="log")
        
        # Modify grid lines and background to fit dark theme
        g.ax.xaxis.grid(True, "minor", linewidth=.25, color='white')  # Minor grid lines in white
        g.ax.yaxis.grid(True, "minor", linewidth=.25, color='white')
        g.despine(left=True, bottom=True)  # Remove left and bottom spines for a cleaner look
        
        # Change figure background color to black
        plt.gcf().set_facecolor('black')

        # Modify the color of the ticks and labels for better contrast
        plt.tick_params(axis='both', colors='white')  # White axis ticks
        plt.xlabel('Distance', fontsize=12, color='white')
        plt.ylabel('Orbital Period', fontsize=12, color='white')
        plt.title('Planets Analysis', fontsize=16, color='white')

        # Save the plot to a BytesIO stream
        img = io.BytesIO()
        plt.savefig(img, format='png', dpi=300, transparent=True)  # Transparent background for easy overlay
        img.seek(0)
        plt.close()
        
        # Return the image as a response
        return send_file(img, mimetype='image/png')

    except Exception as e:
        return jsonify({"error": str(e)}), 500 
    
if __name__ == '__main__':
    app.run(debug=True)
