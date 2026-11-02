"""
Dataset Generator for Rajam and Visakhapatnam (Vizag)
Eliminating the 'Zero-Crime Fallacy' in GuardianAI

Generates realistic geospatial safety incident and environmental infrastructure data
for Rajam and Visakhapatnam to train the Gradient Boosting Classifier fallback.
"""

import os
import random
import numpy as np
import pandas as pd
from pathlib import Path

# Set fixed seed for reproducibility
np.random.seed(42)
random.seed(42)

# Locality Anchors for Rajam (Srikakulam/Vizianagaram district, AP)
RAJAM_LOCALITIES = [
    {"name": "GMRIT Campus & Hostels", "lat": 18.4668, "lon": 83.6622, "type": "Campus", "base_pol_dist": 1.2, "lighting_prob": 0.90, "cctv_prob": 0.85},
    {"name": "Rajam RTC Bus Complex", "lat": 18.4522, "lon": 83.6554, "type": "Transit", "base_pol_dist": 0.3, "lighting_prob": 0.80, "cctv_prob": 0.65},
    {"name": "Main Road & Clock Tower Junction", "lat": 18.4550, "lon": 83.6580, "type": "Commercial", "base_pol_dist": 0.5, "lighting_prob": 0.85, "cctv_prob": 0.60},
    {"name": "Palakonda Road Junction", "lat": 18.4630, "lon": 83.6572, "type": "Commercial", "base_pol_dist": 1.1, "lighting_prob": 0.70, "cctv_prob": 0.40},
    {"name": "Santha Bazar Market Area", "lat": 18.4502, "lon": 83.6605, "type": "Commercial", "base_pol_dist": 0.7, "lighting_prob": 0.65, "cctv_prob": 0.35},
    {"name": "Cheepurupalli Road Stretch", "lat": 18.4450, "lon": 83.6520, "type": "Highway", "base_pol_dist": 1.8, "lighting_prob": 0.45, "cctv_prob": 0.20},
    {"name": "Bobbili Road By-pass", "lat": 18.4600, "lon": 83.6480, "type": "Highway", "base_pol_dist": 2.2, "lighting_prob": 0.40, "cctv_prob": 0.15},
    {"name": "Rajam Industrial & Rice Mill Zone", "lat": 18.4385, "lon": 83.6705, "type": "Industrial", "base_pol_dist": 2.8, "lighting_prob": 0.30, "cctv_prob": 0.10},
    {"name": "Degree College Road", "lat": 18.4580, "lon": 83.6635, "type": "Residential", "base_pol_dist": 1.0, "lighting_prob": 0.70, "cctv_prob": 0.30},
    {"name": "Kothapeta Residential Colony", "lat": 18.4470, "lon": 83.6490, "type": "Residential", "base_pol_dist": 1.4, "lighting_prob": 0.60, "cctv_prob": 0.25},
    {"name": "Vangara Rural Highway Outskirts", "lat": 18.4725, "lon": 83.6595, "type": "Highway", "base_pol_dist": 3.4, "lighting_prob": 0.25, "cctv_prob": 0.05},
    {"name": "GMR Care Hospital Corridor", "lat": 18.4685, "lon": 83.6655, "type": "Transit", "base_pol_dist": 1.3, "lighting_prob": 0.95, "cctv_prob": 0.80},
    {"name": "Rajam Police Station Ward", "lat": 18.4532, "lon": 83.6565, "type": "Transit", "base_pol_dist": 0.1, "lighting_prob": 0.90, "cctv_prob": 0.85},
    {"name": "Saradhi Nagar Colony", "lat": 18.4595, "lon": 83.6690, "type": "Residential", "base_pol_dist": 1.6, "lighting_prob": 0.55, "cctv_prob": 0.20},
]

# Locality Anchors for Visakhapatnam (Vizag, AP)
VIZAG_LOCALITIES = [
    {"name": "Siripuram & Dutt Island", "lat": 17.7212, "lon": 83.3155, "type": "Commercial", "base_pol_dist": 0.4, "lighting_prob": 0.95, "cctv_prob": 0.85},
    {"name": "RK Beach & Marine Drive Promenade", "lat": 17.7142, "lon": 83.3245, "type": "Transit", "base_pol_dist": 0.6, "lighting_prob": 0.90, "cctv_prob": 0.80},
    {"name": "Jagadamba Junction Cinema Area", "lat": 17.7115, "lon": 83.3005, "type": "Commercial", "base_pol_dist": 0.5, "lighting_prob": 0.85, "cctv_prob": 0.70},
    {"name": "Dwaraka Nagar & Complex Bus Depot", "lat": 17.7275, "lon": 83.3102, "type": "Transit", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.75},
    {"name": "Maddilapalem & AU North Campus", "lat": 17.7320, "lon": 83.3280, "type": "Campus", "base_pol_dist": 0.8, "lighting_prob": 0.85, "cctv_prob": 0.65},
    {"name": "MVP Colony Sector 1-9", "lat": 17.7420, "lon": 83.3410, "type": "Residential", "base_pol_dist": 0.7, "lighting_prob": 0.85, "cctv_prob": 0.70},
    {"name": "Rushikonda IT SEZ & Hillside", "lat": 17.7820, "lon": 83.3770, "type": "Campus", "base_pol_dist": 2.5, "lighting_prob": 0.70, "cctv_prob": 0.60},
    {"name": "Rushikonda Beach Isolated Stretches", "lat": 17.7950, "lon": 83.3850, "type": "Highway", "base_pol_dist": 3.8, "lighting_prob": 0.35, "cctv_prob": 0.15},
    {"name": "Gajuwaka Industrial Belt", "lat": 17.6912, "lon": 83.2185, "type": "Industrial", "base_pol_dist": 1.8, "lighting_prob": 0.50, "cctv_prob": 0.25},
    {"name": "Scindia & Container Terminal", "lat": 17.6850, "lon": 83.2800, "type": "Industrial", "base_pol_dist": 3.5, "lighting_prob": 0.30, "cctv_prob": 0.15},
    {"name": "Visakhapatnam Central Railway Station", "lat": 17.7240, "lon": 83.2920, "type": "Transit", "base_pol_dist": 0.4, "lighting_prob": 0.85, "cctv_prob": 0.80},
    {"name": "Health City & Arilova Valley", "lat": 17.7650, "lon": 83.3300, "type": "Transit", "base_pol_dist": 1.5, "lighting_prob": 0.80, "cctv_prob": 0.60},
    {"name": "NAD Kotha Road Flyover Junction", "lat": 17.7400, "lon": 83.2350, "type": "Transit", "base_pol_dist": 0.6, "lighting_prob": 0.80, "cctv_prob": 0.55},
    {"name": "Pendurthi Highway Extension", "lat": 17.8080, "lon": 83.2000, "type": "Highway", "base_pol_dist": 3.2, "lighting_prob": 0.40, "cctv_prob": 0.15},
    {"name": "Bheemili Coastal Highway Corridor", "lat": 17.8900, "lon": 83.4500, "type": "Highway", "base_pol_dist": 4.5, "lighting_prob": 0.30, "cctv_prob": 0.10},
    {"name": "Simhachalam Foothills & Ghat Road", "lat": 17.7680, "lon": 83.2500, "type": "Residential", "base_pol_dist": 2.2, "lighting_prob": 0.45, "cctv_prob": 0.20},
    {"name": "Marripalem & 104 Residential Area", "lat": 17.7450, "lon": 83.2650, "type": "Residential", "base_pol_dist": 1.1, "lighting_prob": 0.70, "cctv_prob": 0.35},
    {"name": "Seethammadhara North Extension", "lat": 17.7380, "lon": 83.3150, "type": "Residential", "base_pol_dist": 0.8, "lighting_prob": 0.80, "cctv_prob": 0.50},
    {"name": "Gopalapatnam Transit Market", "lat": 17.7550, "lon": 83.2150, "type": "Commercial", "base_pol_dist": 1.0, "lighting_prob": 0.75, "cctv_prob": 0.40},
]

TIMES_OF_DAY = ["Morning", "Afternoon", "Evening", "Night", "Late Night"]
TIME_PROBS = [0.22, 0.20, 0.23, 0.20, 0.15]

CRIME_TYPES = [
    "None", "Stalking", "Harassment", "Chain Snatching",
    "Night Safety Complaint", "Verbal Abuse", "Assault",
    "Unsafe Transport", "Eve Teasing", "Theft"
]

def generate_dataset(num_samples_per_locality=60):
    rows = []
    all_localities = [(loc, "Rajam") for loc in RAJAM_LOCALITIES] + [(loc, "Visakhapatnam") for loc in VIZAG_LOCALITIES]

    for loc, city in all_localities:
        for _ in range(num_samples_per_locality):
            # Coordinates with slight realistic jitter (+/- 250m to 600m)
            jitter_lat = np.random.normal(0, 0.0035)
            jitter_lon = np.random.normal(0, 0.0035)
            lat = round(loc["lat"] + jitter_lat, 6)
            lon = round(loc["lon"] + jitter_lon, 6)

            time_of_day = np.random.choice(TIMES_OF_DAY, p=TIME_PROBS)
            is_dark_time = time_of_day in ["Night", "Late Night"]
            is_late_night = (time_of_day == "Late Night")

            # Street lighting: dependent on locality infrastructure & time
            # In day time, street lights might be off, but visibility is daytime ambient.
            # Here street_lighting specifically denotes active street illumination infrastructure.
            lighting_chance = loc["lighting_prob"]
            if is_late_night:
                lighting_chance *= 0.75  # Some lights turned off or broken late at night
            street_lighting = 1 if np.random.rand() < lighting_chance else 0

            # CCTV presence
            cctv_chance = loc["cctv_prob"]
            cctv_nearby = 1 if np.random.rand() < cctv_chance else 0

            # Police station distance
            pol_dist = round(max(0.1, np.random.normal(loc["base_pol_dist"], 0.35)), 2)

            # Crowd density
            if is_late_night:
                crowd_density = int(np.random.uniform(5, 80))
            elif is_dark_time:
                crowd_density = int(np.random.uniform(50, 400))
            else:
                crowd_density = int(np.random.uniform(150, 950))

            # Historical Crime Count:
            # Crucial: 40% of records intentionally have ZERO historical crimes logged,
            # allowing us to test and eliminate the Zero-Crime Fallacy!
            has_zero_crime = (np.random.rand() < 0.40)
            if has_zero_crime:
                crime_count = 0
                crime_type = "None"
            else:
                # Modest historical crime log
                crime_count = int(np.random.exponential(scale=8) + 1)
                crime_count = min(crime_count, 65)
                crime_type = np.random.choice(CRIME_TYPES[1:])

            # Safety Score Calculation (0 to 100)
            # Incorporates environmental infrastructure, time of day, and spatial security:
            # Baseline: 70
            score = 70.0

            # Lighting factor (+20 for well-lit, -25 if dark during night/late night)
            if street_lighting == 1:
                score += 18.0
            else:
                if is_late_night:
                    score -= 32.0
                elif is_dark_time:
                    score -= 22.0
                else:
                    score -= 8.0

            # CCTV factor (+12 if monitored, -5 if absent in commercial/transit)
            if cctv_nearby == 1:
                score += 12.0
            else:
                if loc["type"] in ["Commercial", "Transit"]:
                    score -= 6.0

            # Police distance factor (-5.5 points per km away beyond 0.5km)
            if pol_dist <= 0.5:
                score += 8.0
            elif pol_dist <= 1.2:
                score += 3.0
            else:
                score -= (pol_dist - 1.2) * 5.2

            # Time of day hazard
            if is_late_night:
                score -= 15.0
            elif is_dark_time:
                score -= 6.0
            elif time_of_day == "Morning":
                score += 5.0

            # Area type context
            if loc["type"] in ["Industrial", "Highway"]:
                score -= 8.0
            elif loc["type"] in ["Campus", "Residential"]:
                score += 4.0

            # Historical crime count impact
            # Notice: Crime reduces safety, BUT lack of crime (crime_count == 0) does NOT guarantee safety!
            # If crime_count == 0, the score is determined by the environmental factors above.
            score -= (crime_count * 0.75)

            # Clamp score between 5.0 and 98.5
            final_score = round(float(np.clip(score, 5.0, 98.5)), 1)

            # Deterministic Risk Level Stratification
            if final_score >= 75.0:
                safety_level = "Safe"
            elif final_score >= 50.0:
                safety_level = "Moderate"
            else:
                safety_level = "Unsafe"

            rows.append({
                "city": city,
                "area": loc["name"],
                "latitude": lat,
                "longitude": lon,
                "time_of_day": time_of_day,
                "area_type": loc["type"],
                "street_lighting": street_lighting,
                "cctv_nearby": cctv_nearby,
                "police_distance_km": pol_dist,
                "crowd_density": crowd_density,
                "historical_crime_count": crime_count,
                "crime_type": crime_type,
                "safety_score": final_score,
                "safety_level": safety_level
            })

    df = pd.DataFrame(rows)
    # Shuffle dataset
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    return df

if __name__ == "__main__":
    df = generate_dataset(num_samples_per_locality=55)
    print(f"Total records generated: {len(df)}")
    print(df["city"].value_counts())
    print("\nSafety Level Breakdown:")
    print(df["safety_level"].value_counts())
    print("\nZero Crime Records Breakdown:")
    zero_crime_df = df[df["historical_crime_count"] == 0]
    print(f"Count of 0-crime records: {len(zero_crime_df)}")
    print("Safety levels for 0-crime records (Demonstrating Zero-Crime Fallacy Elimination):")
    print(zero_crime_df["safety_level"].value_counts())
    print(f"Mean score for unlit 0-crime night records: {zero_crime_df[(zero_crime_df['street_lighting']==0) & (zero_crime_df['time_of_day'].isin(['Night', 'Late Night']))]['safety_score'].mean():.2f}")
    print(f"Mean score for well-lit 0-crime records: {zero_crime_df[(zero_crime_df['street_lighting']==1)]['safety_score'].mean():.2f}")

