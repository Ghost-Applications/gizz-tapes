package gizz.tapes.api

val showsJson = """
[    
    {
        "id": "2022-10-07",
        "date": "2022-10-07",
        "venuename": "KEXP Studios",
        "location": "Seattle, WA, USA",
        "title": null,
        "order": 1,
        "poster_url": null,
        "average_rating": 4.5,
        "count_ratings": 6,
        "weighted_rating": 4.475755593392184
    },
    {
        "id": "2022-10-10",
        "date": "2022-10-10",
        "venuename": "Red Rocks Amphitheatre",
        "location": "Morrison, CO, USA",
        "title": "Marathon Night 1",
        "order": 1,
        "poster_url": "https://i.songfishapp.com/kglw/650a917b532a1-red_rocks_2022.jpg",
        "average_rating": 4.7,
        "count_ratings": 43,
        "weighted_rating": 4.6782138438796474
    },
    {
        "id": "2022-10-11",
        "date": "2022-10-11",
        "venuename": "Red Rocks Amphitheatre",
        "location": "Morrison, CO, USA",
        "title": "Marathon Night 2",
        "order": 1,
        "poster_url": "https://i.songfishapp.com/kglw/650a917b532a1-red_rocks_2022.jpg",
        "average_rating": 4.8,
        "count_ratings": 40,
        "weighted_rating": 4.767823558723091
    },
    {
        "id": "2022-10-14",
        "date": "2022-10-14",
        "venuename": "Palace Theatre",
        "location": "St. Paul, MN, USA",
        "title": null,
        "order": 1,
        "poster_url": "https://kglw.net/i/poster-art-1678284680.jpeg",
        "average_rating": 4.8,
        "count_ratings": 6,
        "weighted_rating": 4.6574150676958315
    },
    {
        "id": "2022-10-15",
        "date": "2022-10-15",
        "venuename": "Radius",
        "location": "Chicago, IL, USA",
        "title": null,
        "order": 1,
        "poster_url": "https://i.songfishapp.com/kglw/650a9248536a5-chicago_2022.jpg",
        "average_rating": 4.4,
        "count_ratings": 8,
        "weighted_rating": 4.412649259852024
    }
]
""".trimIndent()

val heroPhotosJson = """
[
    {
        "credit": "John Doe",
        "url": "https://example.com/photo.jpg",
        "vPosition": 50
    }
]
""".trimIndent()

val yearsJson = """
[
    {
        "year": 2022,
        "show_count": 5,
        "poster_url": "https://example.com/poster.jpg"
    },
    {
        "year": 2023,
        "show_count": 10,
        "poster_url": null
    }
]
""".trimIndent()

val statsJson = """
{
    "latest_year": 2024,
    "earliest_year": 2012,
    "total_shows": 100,
    "total_recordings": 200,
    "hours": 500,
    "minutes": 30,
    "sbd_count": 150,
    "aud_count": 50
}
""".trimIndent()

val countriesJson = """
[
    {
        "id": 1,
        "name": "USA",
        "show_count": 100
    }
]
""".trimIndent()

val venuesJson = """
[
    {
        "id": 726,
        "slug": "edgefield-amphitheater",
        "name": "Edgefield Amphitheater",
        "city": "Troutdale",
        "region": "OR",
        "country_id": 1,
        "show_count": 5
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
        "average_rating": 3.7,
        "count_ratings": 3,
        "weighted_rating": 4.117837112143297
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
