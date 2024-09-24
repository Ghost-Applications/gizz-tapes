package gizz.tapes.api

val showsJson = """
[    
    {
        "id": "2024-09-08",
        "date": "2024-09-08",
        "venuename": "Red Rocks Amphitheatre",
        "location": "Morrison, CO, USA",
        "title": "",
        "order": 1,
        "poster_url": "https://kglw.net/i/poster-art-1699403394.jpeg"
    },
    {
        "id": "2024-09-09early",
        "date": "2024-09-09",
        "venuename": "Red Rocks Amphitheatre",
        "location": "Morrison, CO, USA",
        "title": "Early Show",
        "order": 1,
        "poster_url": "https://kglw.net/i/poster-art-1699403422.png"
    },
    {
        "id": "2024-09-09late",
        "date": "2024-09-09",
        "venuename": "Red Rocks Amphitheatre",
        "location": "Morrison, CO, USA",
        "title": "Late Show",
        "order": 2,
        "poster_url": "https://kglw.net/i/poster-art-1699403442.jpeg"
    },
    {
        "id": "2024-09-11",
        "date": "2024-09-11",
        "venuename": "Edgefield Amphitheater",
        "location": "Troutdale, OR, USA",
        "title": "",
        "order": 1,
        "poster_url": "https://kglw.net/i/poster-art-1699403482.jpeg"
    },
    {
        "id": "2024-09-12",
        "date": "2024-09-12",
        "venuename": "Pacific Coliseum",
        "location": "Vancouver, BC, Canada",
        "title": "",
        "order": 1,
        "poster_url": "https://kglw.net/i/poster-art-1699403518.jpeg"
    },
    {
        "id": "2024-09-14",
        "date": "2024-09-14",
        "venuename": "The Gorge Amphitheatre",
        "location": "Quincy, WA, USA",
        "title": "Marathon Show",
        "order": 1,
        "poster_url": "https://kglw.net/i/poster-art-1694538149.jpeg"
    }
]
""".trimIndent()

val showJson = """
    {
        "id": "2024-09-11",
        "date": "2024-09-11",
        "order": 1,
        "poster_url": "https://kglw.net/i/poster-art-1699403482.jpeg",
        "notes": "The \"first set\" took place during an unrelenting rain storm, and following it, the band took a break due to risk of endangering themselves. The Dripping Tap contained an I'm In Your Mind tease at the ending. Boogieman Sam contained The Bitter Boogie teases. Following The Fourth Colour, John Gourley (of Portugal. The Man) came on stage and gave the band shots. Self-Immolate featured a drum solo with a Moby Dick (Led Zeppelin) tease. Supercell was introduced as a song about storms. The Lord of Lightning contained an extended outro with a Cellophane tease. The Balrog was last played 2023-03-20. (85 show gap)\r\n\r\nPoster By Jason Galea\r\nStandard: 600\r\nRainbow Foil: 200",
        "title": "",
        "kglw_net": {
            "id": 1699403482,
            "permalink": "king-gizzard-the-lizard-wizard-september-11-2024-edgefield-amphitheater-troutdale-or-usa.html"
        },
        "venue_id": 726,
        "tour_id": 52,
        "recordings": [
            {
                "id": "kglw2024-09-11.bandcampbootlegger",
                "uploaded_at": "2024-09-15T20:37:30+00:00",
                "type": "SBD",
                "source": "SBD",
                "lineage": "SBD > Bandcamp",
                "taper": "Sam Joseph",
                "files_path_prefix": "https://archive.org/download/kglw2024-09-11.bandcampbootlegger/",
                "internet_archive": {
                    "is_lma": true
                },
                "files": [
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 01 The Dripping Tap (Live).mp3",
                        "length": 961,
                        "title": "The Dripping Tap (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 02 Boogieman Sam (Live).mp3",
                        "length": 484,
                        "title": "Boogieman Sam (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 03 Daily Blues (Live).mp3",
                        "length": 605,
                        "title": "Daily Blues (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 04 Le Risque (Live).mp3",
                        "length": 288,
                        "title": "Le Risque (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 05 Crumbling Castle (Live).mp3",
                        "length": 585,
                        "title": "Crumbling Castle (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 06 The Fourth Colour (Live).mp3",
                        "length": 321,
                        "title": "The Fourth Colour (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 07 Self-Immolate (Live).mp3",
                        "length": 435,
                        "title": "Self-Immolate (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 08 Supercell (Live).mp3",
                        "length": 305,
                        "title": "Supercell (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 09 Organ Farmer (Live).mp3",
                        "length": 216,
                        "title": "Organ Farmer (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 10 Converge (Live).mp3",
                        "length": 370,
                        "title": "Converge (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 11 Witchcraft (Live).mp3",
                        "length": 410,
                        "title": "Witchcraft (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 12 Sad Pilot (Live).mp3",
                        "length": 363,
                        "title": "Sad Pilot (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 13 The Reticent Raconteur (Live).mp3",
                        "length": 59,
                        "title": "The Reticent Raconteur (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 14 The Lord of Lightning (Live).mp3",
                        "length": 312,
                        "title": "The Lord of Lightning (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 15 The Balrog (Live).mp3",
                        "length": 213,
                        "title": "The Balrog (Live)"
                    },
                    {
                        "filename": "King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater- Troutdale- OR (9-11-24)/King Gizzard & The Lizard Wizard - Live at Edgefield Amphiteater, Troutdale, OR (9-11-24) - 16 Field of Vision (Live).mp3",
                        "length": 310,
                        "title": "Field of Vision (Live)"
                    }
                ]
            }
        ]
    }
""".trimIndent()
