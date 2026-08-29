'use client';

import type { TrendPoint } from '@revexa/core';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export function QualityTrend({ points }: { points: TrendPoint[] }) {
  const data = points.map((point) => ({
    ...point,
    label: new Date(point.date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Solution quality over time</CardTitle>
        <CardDescription>
          {data.length < 2
            ? 'Two days of reviews and a trend line appears here.'
            : 'Average review score per day. The shape matters more than any single point.'}
        </CardDescription>
      </CardHeader>
      <CardContent className="pl-0">
        <div className="h-56 w-full">
          {data.length === 0 ? (
            <div className="text-muted-foreground flex h-full items-center justify-center text-sm">
              No reviews yet.
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
                <defs>
                  <linearGradient id="score-fill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="var(--chart-1)" stopOpacity={0.32} />
                    <stop offset="100%" stopColor="var(--chart-1)" stopOpacity={0.02} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" vertical={false} />
                <XAxis
                  dataKey="label"
                  tickLine={false}
                  axisLine={false}
                  tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }}
                  minTickGap={16}
                />
                <YAxis
                  domain={[0, 100]}
                  width={38}
                  tickLine={false}
                  axisLine={false}
                  tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }}
                />
                <Tooltip
                  cursor={{ stroke: 'var(--border)' }}
                  contentStyle={{
                    background: 'var(--popover)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-md)',
                    fontSize: 12,
                    color: 'var(--popover-foreground)',
                  }}
                  formatter={(value, name) => [String(value), name === 'averageScore' ? 'Average score' : String(name)]}
                />
                <Area
                  type="monotone"
                  dataKey="averageScore"
                  stroke="var(--chart-1)"
                  strokeWidth={2}
                  fill="url(#score-fill)"
                  dot={data.length <= 12 ? { r: 3, fill: 'var(--chart-1)' } : false}
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
