import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import axios from 'axios'
import Dashboard from '../Dashboard.vue'

vi.mock('axios')

const mockMenuItems = [
  { id: 101, name: 'Chicken Alfredo', category: 'PASTA', imageName: 'chicken_alfredo.jpg' },
  { id: 102, name: 'Aglio e Olio', category: 'PASTA', imageName: null },
]

const mockQuestions = [
  { id: 1, text: 'Question 1 — Mood Association', type: 'RADIO' },
  { id: 2, text: 'Question 2 — Weather Association', type: 'RADIO' },
]

const mockDemographics = {
  totalParticipants: 25,
  ageGroupCounts: {
    '18–20': 5,
    '21–23': 12,
    '24–26': 5,
    '27–30': 2,
    '31 and above': 1,
  },
  diningFrequencyCounts: {
    'Several times a week': 8,
    'Once a week': 10,
    'Several times a month': 5,
    'Once a month': 2,
    'Less than once a month': 0,
  },
}

const mockCombinedAnalytics = {
  analyticsData: {},
  stats: {
    globalTotal: 25,
    itemTotal: 10,
    engagementPct: 40,
    positiveCount: 8,
    neutralCount: 1,
    negativeCount: 1,
    positivePct: 80,
    neutralPct: 10,
    negativePct: 10,
    topMood: 'Comfort',
    topMoodScore: 4.8,
    topWeather: 'Rainy',
    topWeatherScore: 4.6,
    avgSuitabilityScore: 4.5,
    topKeywords: ['creamy', 'comfort'],
  },
  moodAnalytics: {
    title: 'Question 1 — Mood Association',
    prompt: 'How suitable is Chicken Alfredo for each mood?',
    topRowLabel: 'Comfort',
    topRowScore: 4.8,
    totalEvaluators: 10,
    rows: [
      {
        id: 'energy',
        label: 'Energy (Wants something energizing)',
        shortLabel: 'Energy',
        avgRating: 3.2,
        totalVotes: 10,
        suitabilityPct: 40,
        distribution: { 1: 1, 2: 2, 3: 3, 4: 2, 5: 2 },
      },
      {
        id: 'comfort',
        label: 'Comfort (Wants something warm or familiar)',
        shortLabel: 'Comfort',
        avgRating: 4.8,
        totalVotes: 10,
        suitabilityPct: 100,
        distribution: { 1: 0, 2: 0, 3: 0, 4: 2, 5: 8 },
      },
      {
        id: 'refreshing',
        label: 'Refreshing (Wants something light or cooling)',
        shortLabel: 'Refreshing',
        avgRating: 2.1,
        totalVotes: 10,
        suitabilityPct: 10,
        distribution: { 1: 5, 2: 3, 3: 1, 4: 1, 5: 0 },
      },
      {
        id: 'healthy',
        label: 'Healthy (Wants a healthier choice)',
        shortLabel: 'Healthy',
        avgRating: 2.5,
        totalVotes: 10,
        suitabilityPct: 20,
        distribution: { 1: 3, 2: 4, 3: 1, 4: 2, 5: 0 },
      },
      {
        id: 'treat',
        label: 'Treat (Wants something enjoyable or indulgent)',
        shortLabel: 'Treat',
        avgRating: 4.6,
        totalVotes: 10,
        suitabilityPct: 90,
        distribution: { 1: 0, 2: 0, 3: 1, 4: 2, 5: 7 },
      },
      {
        id: 'focused',
        label: 'Focused (Wants to concentrate or study)',
        shortLabel: 'Focused',
        avgRating: 3.0,
        totalVotes: 10,
        suitabilityPct: 30,
        distribution: { 1: 2, 2: 2, 3: 3, 4: 2, 5: 1 },
      },
      {
        id: 'familiar',
        label: 'Familiar (Wants a safe, familiar choice)',
        shortLabel: 'Familiar',
        avgRating: 4.5,
        totalVotes: 10,
        suitabilityPct: 90,
        distribution: { 1: 0, 2: 1, 3: 0, 4: 3, 5: 6 },
      },
      {
        id: 'adventurous',
        label: 'Adventurous (Wants to try something new)',
        shortLabel: 'Adventurous',
        avgRating: 2.8,
        totalVotes: 10,
        suitabilityPct: 20,
        distribution: { 1: 3, 2: 3, 3: 2, 4: 1, 5: 1 },
      },
      {
        id: 'quick',
        label: 'Quick (Wants something convenient)',
        shortLabel: 'Quick',
        avgRating: 3.4,
        totalVotes: 10,
        suitabilityPct: 50,
        distribution: { 1: 1, 2: 2, 3: 2, 4: 3, 5: 2 },
      },
    ],
  },
  weatherAnalytics: {
    title: 'Question 2 — Weather Association',
    prompt: 'How suitable is Chicken Alfredo for each weather condition?',
    topRowLabel: 'Rainy',
    topRowScore: 4.6,
    totalEvaluators: 10,
    rows: [
      {
        id: 'hot_sunny',
        label: 'Hot/Sunny',
        shortLabel: 'Hot/Sunny',
        avgRating: 2.3,
        totalVotes: 10,
        suitabilityPct: 20,
        distribution: { 1: 4, 2: 3, 3: 1, 4: 2, 5: 0 },
      },
      {
        id: 'hot_humid',
        label: 'Hot/Humid',
        shortLabel: 'Hot/Humid',
        avgRating: 2.0,
        totalVotes: 10,
        suitabilityPct: 10,
        distribution: { 1: 5, 2: 3, 3: 1, 4: 1, 5: 0 },
      },
      {
        id: 'rainy',
        label: 'Rainy',
        shortLabel: 'Rainy',
        avgRating: 4.6,
        totalVotes: 10,
        suitabilityPct: 90,
        distribution: { 1: 0, 2: 0, 3: 1, 4: 2, 5: 7 },
      },
      {
        id: 'cool_dry',
        label: 'Cool Dry',
        shortLabel: 'Cool Dry',
        avgRating: 4.4,
        totalVotes: 10,
        suitabilityPct: 80,
        distribution: { 1: 0, 2: 1, 3: 1, 4: 3, 5: 5 },
      },
    ],
  },
  demographics: mockDemographics,
  recentResponses: [
    {
      userId: 'user-abc-123456',
      moodRatings: { Comfort: 5, Treat: 5, Energy: 4, Familiar: 4 },
      weatherRatings: { Rainy: 5, 'Cool Dry': 5, 'Hot/Sunny': 2 },
      textFeedback: 'Super creamy and comforting!',
    },
    {
      userId: 'user-xyz-789012',
      moodRatings: { Comfort: 4, Treat: 4, Energy: 3 },
      weatherRatings: { Rainy: 4, 'Cool Dry': 4 },
      textFeedback: null,
    },
  ],
}

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 30))

describe('Dashboard.vue - Analytics View with Survey Taker Data', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    ;(axios.post as any).mockImplementation((url: string) => {
      if (url === '/api/admin/login') {
        return Promise.resolve({ data: { token: 'mock-jwt-token' } })
      }
      return Promise.resolve({ data: {} })
    })
    ;(axios.get as any).mockImplementation((url: string) => {
      if (url === '/menu-items') {
        return Promise.resolve({ data: mockMenuItems })
      }
      if (url === '/questions/all') {
        return Promise.resolve({ data: mockQuestions })
      }
      if (url === '/api/stats/baseline') {
        return Promise.resolve({ data: 25 })
      }
      if (url === '/analytics/demographics') {
        return Promise.resolve({ data: mockDemographics })
      }
      if (url.startsWith('/analytics/combined/')) {
        return Promise.resolve({ data: mockCombinedAnalytics })
      }
      return Promise.resolve({ data: {} })
    })
    axios.defaults = { headers: { common: {} } } as any
  })

  it('authenticates and displays the Analytics view with Section 1 Demographics and Section 2 Grid Data', async () => {
    const wrapper = mount(Dashboard)
    await flushPromises()

    // 1. Log in as admin
    const usernameInput = wrapper.find('input[type="text"]')
    const passwordInput = wrapper.find('input[type="password"]')
    await usernameInput.setValue('admin')
    await passwordInput.setValue('password')
    await wrapper.find('form.login-form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Food Preferences Survey')
    expect(wrapper.text()).toContain('Analytics View')

    // 2. Verify KPI Cards show accurate survey taker metrics
    expect(wrapper.text()).toContain('PARTICIPANTS')
    expect(wrapper.text()).toContain('TOTAL RESPONSES')
    expect(wrapper.text()).toContain('40% of 25 participants')
    expect(wrapper.text()).toContain('SUITABILITY RATE')
    expect(wrapper.text()).toContain('NEUTRAL')
    expect(wrapper.text()).toContain('NEEDS ATTENTION')
    expect(wrapper.findAll('.new-kpi-card').length).toBe(5)

    // 3. Verify Section 1 Demographic Profile Card
    expect(wrapper.text()).toContain('SECTION 1 — Survey Respondent Profile')
    expect(wrapper.text()).toContain('Age Group Distribution')
    expect(wrapper.text()).toContain('18–20')
    expect(wrapper.text()).toContain('21–23')
    expect(wrapper.text()).toContain('Dining Frequency Distribution')
    expect(wrapper.text()).toContain('Several times a week')
    expect(wrapper.text()).toContain('Once a week')

    // 4. Verify Question 1 — Mood Association with 9 moods
    expect(wrapper.text()).toContain('Question 1 — Mood Association')
    expect(wrapper.text()).toContain('Comfort')
    expect(wrapper.text()).toContain('Treat')
    expect(wrapper.text()).toContain('Energy')
    expect(wrapper.text()).toContain('Refreshing')
    expect(wrapper.text()).toContain('Healthy')
    expect(wrapper.text()).toContain('Focused')
    expect(wrapper.text()).toContain('Familiar')
    expect(wrapper.text()).toContain('Adventurous')
    expect(wrapper.text()).toContain('Quick')
    expect(wrapper.text()).toContain('4.8 ★')
    expect(wrapper.text()).toContain('100%')
    expect(wrapper.text()).toContain('Score Track & Vote Spread')
    expect(wrapper.text()).toContain('5:8')
    expect(wrapper.find('.dist-ticks').attributes('title')).toContain('Vote spread')
    expect(wrapper.text()).toContain('Comfort is the leading mood association')

    // 5. Verify Question 2 — Weather Association with 4 weather conditions
    expect(wrapper.text()).toContain('Question 2 — Weather Association')
    expect(wrapper.text()).toContain('Rainy')
    expect(wrapper.text()).toContain('Cool Dry')
    expect(wrapper.text()).toContain('Hot/Sunny')
    expect(wrapper.text()).toContain('Hot/Humid')
    expect(wrapper.text()).toContain('4.6 ★')
    expect(wrapper.text()).toContain('90%')
    expect(wrapper.text()).toContain('Rainy is the optimal weather condition')

    // 6. Verify Individual Survey Taker Evaluations Log
    expect(wrapper.text()).toContain('Survey Takers — Individual Evaluations')
    expect(wrapper.text()).toContain('Respondent #1')
    expect(wrapper.text()).toContain('Respondent #2')
    expect(wrapper.text()).toContain('Comfort: 5★')
    expect(wrapper.text()).toContain('Rainy: 5★')
    expect(wrapper.text()).toContain('Super creamy and comforting!')
  })

  it('normalizes and displays age group distribution when keys use standard hyphens or HTML entities', async () => {
    const rawHyphenDemographics = {
      totalParticipants: 10,
      globalParticipants: 10,
      ageGroupCounts: {
        '18-20': 4,
        '21-23': 6,
      },
      diningFrequencyCounts: {
        'Once a week': 10,
      },
    }

    ;(axios.get as any).mockImplementation((url: string) => {
      if (url === '/menu-items') return Promise.resolve({ data: mockMenuItems })
      if (url === '/questions/all') return Promise.resolve({ data: mockQuestions })
      if (url === '/api/stats/baseline') return Promise.resolve({ data: 10 })
      if (url === '/analytics/demographics') return Promise.resolve({ data: rawHyphenDemographics })
      if (url.startsWith('/analytics/combined/')) {
        return Promise.resolve({
          data: {
            ...mockCombinedAnalytics,
            demographics: rawHyphenDemographics,
          },
        })
      }
      return Promise.resolve({ data: {} })
    })

    const wrapper = mount(Dashboard)
    await flushPromises()

    const usernameInput = wrapper.find('input[type="text"]')
    const passwordInput = wrapper.find('input[type="password"]')
    await usernameInput.setValue('admin')
    await passwordInput.setValue('password')
    await wrapper.find('form.login-form').trigger('submit')
    await flushPromises()

    // 18-20 (4 respondents = 40%) should match option '18–20'
    expect(wrapper.text()).toContain('18–20')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('40%')

    // 21-23 (6 respondents = 60%) should match option '21–23'
    expect(wrapper.text()).toContain('21–23')
    expect(wrapper.text()).toContain('6')
    expect(wrapper.text()).toContain('60%')
  })

  it('restores authenticated session on page refresh and syncs analytics results', async () => {
    localStorage.setItem('admin_token', 'persisted-jwt-token')

    const wrapper = mount(Dashboard)
    await flushPromises()

    // Does not display login wrapper
    expect(wrapper.find('.login-wrapper').exists()).toBe(false)

    // Displays dashboard with synced analytics
    expect(wrapper.text()).toContain('Analytics View')
    expect(wrapper.text()).toContain('Chicken Alfredo')
    expect(wrapper.text()).toContain('SECTION 1 — Survey Respondent Profile')

    // Authorization header was automatically configured with stored token
    expect(axios.defaults.headers.common['Authorization']).toBe('Bearer persisted-jwt-token')
  })
})