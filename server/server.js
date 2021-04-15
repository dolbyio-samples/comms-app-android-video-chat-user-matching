const express = require('express')
const axios = require('axios')
const app = express()
require('dotenv').config()

const port = process.env.PORT || 3000 // use Heroku-assigned port or 3000 for local development

app.use(express.json()) // for parsing application/json


const url_jwt = "https://api.voxeet.com/v1"
const url_client = "https://session.voxeet.com/v1"
const key = process.env.APP_KEY
const secret = process.env.APP_SECRET

const credentials = new Buffer.from(key + ':' + secret)
  .toString('base64');

const options = {
    headers: {
        'Cache-Control': 'no-cache',
        'Content-Type': 'application/json',
        'Authorization': 'Basic ' + credentials
    }
}

const body = {
    'grant_type': 'client_credentials'
}


// get an access token to send to the client 
app.get('/token/access', (req, res) => {
    axios.post(url_client + '/oauth2/token', body, options).then((response) => {
        res.send(response.data.access_token)
    }, (error) => {
        console.log(error)
        res.status(error.response.status).send({ 'message': 'error obtaining token'})
    })
})

/* UNCOMMENT BELOW TO TEST GET CONFERENCE LIST CALL */

// axios.post(url_jwt + '/auth/token', body, options)
//     .then((response) => {
//         console.log(response.data)
//         return response.data.access_token
//     }, 
//     (error) => {
//         console.log(error)
//     })
//     .then(function(token) {
//         const config = {
//             headers: {
//                 'Authorization': 'Bearer ' + token
//             },
//             params: {
//                 active: false
//             }
//         }
//         axios.get(url_jwt + '/monitor/conferences', config)
//             .then((response) => {
//                 var conferences = []

//                 Array.from(response.data.conferences).forEach((conference) => {
//                     conferences.push({
//                         'alias': conference.alias,
//                         'confId': conference.confId,
//                         'dolbyVoice': conference.dolbyVoice,
//                         'duration': conference.duration,
//                         'owner': conference.owner.metadata.externalName
//                     })
//                 })
//                 console.log(conferences)
//                 // console.log(response.data)
//             }, 
//             (error) => {
//                 console.log(error)
//         })
//     })


app.post('/conference/list', (req, res) => {
    const match_alias = req.body[0].query
    console.log(match_alias)

    get_headers().then((headers) => {
        const config = {
            headers,
            params: {
                active: true,
                alias: `${match_alias}.*`
            }
        }

        axios.get(url_jwt + '/monitor/conferences', config)
            .then((response) => {
                var conferences = []
                console.log(response.data.conferences)

                Array.from(response.data.conferences).forEach((conference) => {
                    if (conference.nbUsers > 1) {
                        return
                    }
                    let shortAlias = conference.alias.substring(match_alias.length)
                    var duration = conference.duration
                    if (!duration) {
                        console.log("undefined duration")
                        duration = Date.now() - conference.start
                        console.log(duration)
                    }
                    console.log(shortAlias)
                    conferences.push({
                        'alias': shortAlias,
                        'confId': conference.confId,
                        'dolbyVoice': conference.dolbyVoice,
                        'duration': duration,
                        'owner': conference.owner.metadata.externalName
                    })
                })
                console.log(conferences)
                res.json(conferences)
            }, 
            (error) => {
                console.log(error)
        })
    }, (error) => {
        console.log(error)
        res.status(error.response.status).send({ 'message': 'error getting headers'})
    })

}, (error) => {
    console.log(error)
    res.status(error.response.status).send({ 'message': 'error getting conference list'})
})

app.post('/conference/exists', (req, res) => {
    const alias = req.body.alias 

    get_headers().then((headers) => {
        const config = {
            headers,
            params: {
                active: true,
                alias: alias
            }
        }
        axios.get(url_jwt + '/monitor/conferences', config)
            .then((response) => {
                if (response.data.conferences.length > 0) {
                    result = {
                        'exists': true
                    }
                    res.send(result)
                }
                else {
                    result = {
                        'exists': false
                    }
                    res.send(result)
                }
            }, (error) => {
                res.status(error.response.status).send({ 'message': 'error getting conferences'})
            })
    }, (error) => {
        res.status(error.response.status).send({ 'message': 'error getting headers'})
    })
}, (error) => {
    res.status(error.response.status).send({ 'message': 'bad request'})
})

async function get_headers() {
    const response = await axios.post(url_jwt + '/auth/token', body, options)
    const token = response.data.access_token
    
    const headers = {
                'Authorization': 'Bearer ' + token
            }
    return headers
}



app.listen(port)




